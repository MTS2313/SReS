package br.com.sres.reports;

import br.com.sres.accounts.AccountEntity;
import br.com.sres.accounts.AccountRepository;
import br.com.sres.accounts.AccountService;
import br.com.sres.accounts.AccountStatus;
import br.com.sres.storage.StorageService;
import br.com.sres.usage.QuotaService;
import br.com.sres.usage.ReservationResponse;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ReportService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final List<String> REPORT_TYPES = List.of("EXECUTIVE_SUMMARY", "DETAILED_ANALYSIS", "STRUCTURED_EXTRACTION");

    private final AccountRepository accounts;
    private final AccountService accountService;
    private final QuotaService quotas;
    private final StorageService storage;
    private final JdbcTemplate jdbc;

    public ReportService(AccountRepository accounts, AccountService accountService, QuotaService quotas,
                         StorageService storage, JdbcTemplate jdbc) {
        this.accounts = accounts;
        this.accountService = accountService;
        this.quotas = quotas;
        this.storage = storage;
        this.jdbc = jdbc;
    }

    @Transactional
    public ReportResponse create(Jwt jwt, String type, String description, MultipartFile file, String requestedKey) {
        validateText(type, description);
        Input input = validateFile(file);
        AccountEntity account = accountFor(jwt);
        return createForAccount(account.getId(), jwt.getSubject(), type, description, file, requestedKey, "API");
    }

    @Transactional
    public ReportResponse createForAccount(UUID accountId, String actor, String type, String description, MultipartFile file,
                                           String requestedKey, String origin) {
        validateText(type, description);
        Input input = validateFile(file);
        AccountEntity account = accounts.findById(accountId).orElseThrow(() -> new NotFoundException("Conta não encontrada"));
        if (account.getStatus() == AccountStatus.BLOCKED) throw new BlockedAccountException();
        String key = requestedKey == null || requestedKey.isBlank() ? UUID.randomUUID().toString() : requestedKey;
        lockIdempotency(account.getId(), key);
        ReportResponse existing = existing(account.getId(), key);
        if (existing != null) return existing;

        String temporaryKey = null;
        String finalKey = null;
        ReservationResponse reservation = null;
        UUID reportId = UUID.randomUUID();
        try {
            if (input != null) temporaryKey = storage.putTemporary(input.bytes(), "application/pdf");
            reservation = quotas.reserveForAccount(account.getId(), key, actor);
            jdbc.update("insert into reports (id, account_id, report_type, origin, description, extracted_text, status, reservation_id) values (?, ?, ?, ?, ?, ?, 'PENDING', ?)",
                    reportId, account.getId(), type, origin, description, input == null ? null : input.extractedText(), reservation.id());
            if (input != null) {
                finalKey = "accounts/" + account.getId() + "/reports/" + reportId + "/input.pdf";
                finalKey = storage.promote(temporaryKey, account.getId(), reportId);
                jdbc.update("insert into report_files (report_id, file_kind, bucket_name, object_key, content_type, size_bytes, temporary) values (?, 'INPUT_PDF', ?, ?, 'application/pdf', ?, false)",
                        reportId, storage.bucket(), finalKey, input.bytes().length);
                temporaryKey = null;
            }
            jdbc.update("insert into report_idempotency (account_id, idempotency_key, report_id, expires_at) values (?, ?, ?, current_timestamp + interval '24 hours')",
                    account.getId(), key, reportId);
            return response(reportId);
        } catch (RuntimeException exception) {
            cleanup(temporaryKey, finalKey);
            if (reservation != null) {
                try { quotas.releaseForAccount(reservation.id(), account.getId()); } catch (RuntimeException ignored) { }
            }
            throw exception;
        }
    }

    @Transactional
    public ReportPageResponse list(Jwt jwt, int page, int size, String sort) {
        if (page < 0 || size < 1 || size > 100) throw new BadRequestException("Paginação inválida");
        AccountEntity account = accountFor(jwt);
        String order = "asc".equalsIgnoreCase(sort) ? "ASC" : "DESC";
        long total = jdbc.queryForObject("select count(*) from reports where account_id = ?", Long.class, account.getId());
        List<ReportResponse> rows = jdbc.query("select r.id, r.report_type, r.origin, r.description, r.status, r.created_at, exists(select 1 from report_files f where f.report_id = r.id and f.file_kind = 'INPUT_PDF') as input_available from reports r where r.account_id = ? order by r.created_at " + order + ", r.id " + order + " limit ? offset ?",
                (rs, row) -> new ReportResponse(rs.getObject("id", UUID.class), rs.getString("report_type"), rs.getString("origin"), rs.getString("description"), rs.getString("status"), rs.getBoolean("input_available"), rs.getTimestamp("created_at").toInstant()), account.getId(), size, page * size);
        return new ReportPageResponse(rows, page, size, total);
    }

    @Transactional(readOnly = true)
    public ReportPageResponse adminList(int page, int size, String sort) {
        if (page < 0 || size < 1 || size > 100) throw new BadRequestException("Paginação inválida");
        String order = "asc".equalsIgnoreCase(sort) ? "ASC" : "DESC";
        long total = jdbc.queryForObject("select count(*) from reports", Long.class);
        List<ReportResponse> rows = jdbc.query("select r.id, r.report_type, r.origin, r.description, r.status, r.created_at, exists(select 1 from report_files f where f.report_id = r.id and f.file_kind = 'INPUT_PDF') as input_available from reports r order by r.created_at " + order + ", r.id " + order + " limit ? offset ?",
                (rs, row) -> new ReportResponse(rs.getObject("id", UUID.class), rs.getString("report_type"), rs.getString("origin"), rs.getString("description"), rs.getString("status"), rs.getBoolean("input_available"), rs.getTimestamp("created_at").toInstant()), size, page * size);
        return new ReportPageResponse(rows, page, size, total);
    }

    @Transactional
    public ReportResponse get(Jwt jwt, UUID id) {
        return findOwned(accountFor(jwt).getId(), id);
    }

    @Transactional
    public DownloadedFile input(Jwt jwt, UUID id) {
        UUID accountId = accountFor(jwt).getId();
        findOwned(accountId, id);
        var file = jdbc.queryForObject("select object_key, content_type from report_files where report_id = ? and file_kind = 'INPUT_PDF'", (rs, row) -> new FileRow(rs.getString("object_key"), rs.getString("content_type")), id);
        if (file == null) throw new NotFoundException("Arquivo de entrada não encontrado");
        try (var object = storage.get(file.objectKey())) {
            return new DownloadedFile(object.readAllBytes(), file.contentType());
        } catch (IOException exception) {
            throw new StorageService.StorageException("Falha ao ler arquivo", exception);
        }
    }

    @Transactional
    public DownloadedFile output(Jwt jwt, UUID id) {
        UUID accountId = accountFor(jwt).getId();
        findOwned(accountId, id);
        var file = jdbc.queryForObject("select object_key, content_type from report_files where report_id = ? and file_kind = 'OUTPUT_MARKDOWN'", (rs, row) -> new FileRow(rs.getString("object_key"), rs.getString("content_type")), id);
        if (file == null) throw new NotFoundException("Resultado não encontrado");
        try (var object = storage.get(file.objectKey())) {
            return new DownloadedFile(object.readAllBytes(), file.contentType());
        } catch (IOException exception) {
            throw new StorageService.StorageException("Falha ao ler resultado", exception);
        }
    }

    private AccountEntity accountFor(Jwt jwt) {
        accountService.me(jwt);
        return accounts.findBySubject(jwt.getSubject()).orElseThrow(() -> new NotFoundException("Conta não encontrada"));
    }

    private ReportResponse existing(UUID accountId, String key) {
        List<ExistingResponse> rows = jdbc.query("select r.id, r.report_type, r.origin, r.description, r.status, r.created_at, exists(select 1 from report_files f where f.report_id = r.id and f.file_kind = 'INPUT_PDF') as input_available, i.expires_at from report_idempotency i join reports r on r.id = i.report_id where i.account_id = ? and i.idempotency_key = ? for update",
                (rs, row) -> new ExistingResponse(new ReportResponse(rs.getObject("id", UUID.class), rs.getString("report_type"), rs.getString("origin"), rs.getString("description"), rs.getString("status"), rs.getBoolean("input_available"), rs.getTimestamp("created_at").toInstant()), rs.getTimestamp("expires_at").toInstant()), accountId, key);
        if (rows.isEmpty()) return null;
        ExistingResponse row = rows.getFirst();
        if (row.expiresAt().isAfter(Instant.now())) return row.response();
        jdbc.update("update report_idempotency set idempotency_key = concat(idempotency_key, ':expired:', report_id::text) where account_id = ? and idempotency_key = ?", accountId, key);
        return null;
    }

    private ReportResponse findOwned(UUID accountId, UUID id) {
        List<ReportResponse> rows = jdbc.query("select r.id, r.report_type, r.origin, r.description, r.status, r.created_at, exists(select 1 from report_files f where f.report_id = r.id and f.file_kind = 'INPUT_PDF') as input_available from reports r where r.id = ? and r.account_id = ?",
                (rs, row) -> new ReportResponse(rs.getObject("id", UUID.class), rs.getString("report_type"), rs.getString("origin"), rs.getString("description"), rs.getString("status"), rs.getBoolean("input_available"), rs.getTimestamp("created_at").toInstant()), id, accountId);
        if (rows.isEmpty()) throw new NotFoundException("Relatório não encontrado");
        return rows.getFirst();
    }

    private ReportResponse response(UUID id) { return findById(id); }

    private ReportResponse findById(UUID id) {
        return jdbc.queryForObject("select r.id, r.report_type, r.origin, r.description, r.status, r.created_at, exists(select 1 from report_files f where f.report_id = r.id and f.file_kind = 'INPUT_PDF') as input_available from reports r where r.id = ?", (rs, row) -> new ReportResponse(rs.getObject("id", UUID.class), rs.getString("report_type"), rs.getString("origin"), rs.getString("description"), rs.getString("status"), rs.getBoolean("input_available"), rs.getTimestamp("created_at").toInstant()), id);
    }

    private void lockIdempotency(UUID accountId, String key) {
        jdbc.query("select pg_advisory_xact_lock(hashtextextended(?, 0))", ps -> ps.setString(1, accountId + ":" + key), rs -> null);
    }

    private static void validateText(String type, String description) {
        if (!REPORT_TYPES.contains(type)) throw new BadRequestException("Tipo de relatório inválido");
        if (description == null || description.isBlank()) throw new BadRequestException("Descrição é obrigatória");
    }

    private static Input validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > MAX_FILE_SIZE || !"application/pdf".equalsIgnoreCase(file.getContentType())) throw new BadRequestException("PDF inválido");
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length < 5 || !new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")) throw new BadRequestException("PDF inválido");
            try (var document = Loader.loadPDF(bytes)) {
                if (document.getNumberOfPages() > 50) throw new BadRequestException("PDF excede 50 páginas");
                return new Input(bytes, new PDFTextStripper().getText(document));
            }
        } catch (IOException exception) {
            throw new BadRequestException("PDF inválido");
        }
    }

    private void cleanup(String temporaryKey, String finalKey) {
        try { storage.delete(temporaryKey); } catch (RuntimeException ignored) { }
        try { storage.delete(finalKey); } catch (RuntimeException ignored) { }
    }

    public record DownloadedFile(byte[] content, String contentType) { }
    private record Input(byte[] bytes, String extractedText) { }
    private record FileRow(String objectKey, String contentType) { }
    private record ExistingResponse(ReportResponse response, Instant expiresAt) { }

    public static class NotFoundException extends RuntimeException { public NotFoundException(String message) { super(message); } }
    public static class BadRequestException extends RuntimeException { public BadRequestException(String message) { super(message); } }
    public static class BlockedAccountException extends RuntimeException { public BlockedAccountException() { super("Conta bloqueada não pode criar relatório"); } }
}
