package br.com.sres.processing;

import br.com.sres.ollama.GenerationResult;
import br.com.sres.ollama.ReportGenerator;
import br.com.sres.storage.StorageService;
import br.com.sres.usage.QuotaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
public class ReportProcessingWorker {
    private static final int MAX_ATTEMPTS = 2;
    private final JdbcTemplate jdbc;
    private final org.springframework.beans.factory.ObjectProvider<ReportGenerator> generator;
    private final StorageService storage;
    private final QuotaService quotas;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transactions;
    private final long recoveryMinutes;
    private final boolean enabled;

    public ReportProcessingWorker(JdbcTemplate jdbc, org.springframework.beans.factory.ObjectProvider<ReportGenerator> generator, StorageService storage, QuotaService quotas,
                                  ApplicationEventPublisher events,
                                  org.springframework.transaction.PlatformTransactionManager transactionManager,
                                  @Value("${sres.processing.recovery-minutes:30}") long recoveryMinutes,
                                  @Value("${sres.processing.enabled:true}") boolean enabled) {
        this.jdbc = jdbc;
        this.generator = generator;
        this.storage = storage;
        this.quotas = quotas;
        this.events = events;
        this.transactions = new TransactionTemplate(transactionManager);
        this.recoveryMinutes = recoveryMinutes;
        this.enabled = enabled;
    }

    @Scheduled(initialDelayString = "${sres.processing.initial-delay-ms:1000}",
            fixedDelayString = "${sres.processing.interval-ms:1000}")
    public void scheduledProcess() {
        if (enabled) processOne();
    }

    public void processOne() {
        if (!enabled) return;
        ReportGenerator activeGenerator = generator.getIfAvailable();
        if (activeGenerator == null) return;
        WorkItem item = transactions.execute(status -> claimOne());
        if (item == null) return;
        long started = System.currentTimeMillis();
        try {
            GenerationResult result = activeGenerator.generate(item.type(), prompt(item));
            String outputKey = storage.putOutput(result.markdown().getBytes(StandardCharsets.UTF_8), item.accountId(), item.reportId());
            try {
                transactions.execute(status -> { complete(item, result, outputKey, System.currentTimeMillis() - started); return null; });
            } catch (RuntimeException exception) {
                storage.delete(outputKey);
                throw exception;
            }
        } catch (RuntimeException exception) {
            transactions.execute(status -> { fail(item, exception, System.currentTimeMillis() - started); return null; });
        }
    }

    WorkItem claimOne() {
        jdbc.update("update reports set status = 'PENDING', updated_at = current_timestamp where status = 'PROCESSING' and updated_at < current_timestamp - (? * interval '1 minute')", recoveryMinutes);
        List<WorkItem> rows = jdbc.query("select id, account_id, report_type, description, extracted_text, reservation_id from reports where status = 'PENDING' order by created_at, id for update skip locked limit 1",
                (rs, row) -> new WorkItem(rs.getObject("id", UUID.class), rs.getObject("account_id", UUID.class), rs.getString("report_type"), rs.getString("description"), rs.getString("extracted_text"), rs.getObject("reservation_id", UUID.class)));
        if (rows.isEmpty()) return null;
        WorkItem item = rows.getFirst();
        jdbc.update("update reports set status = 'PROCESSING', updated_at = current_timestamp where id = ?", item.reportId());
        int attempt = jdbc.queryForObject("select coalesce(max(attempt_number), 0) + 1 from report_attempts where report_id = ?", Integer.class, item.reportId());
        jdbc.update("insert into report_attempts (report_id, attempt_number, status, started_at) values (?, ?, 'RUNNING', current_timestamp)", item.reportId(), attempt);
        return item;
    }

    void complete(WorkItem item, GenerationResult result, String outputKey, long durationMs) {
        jdbc.update("insert into report_files (report_id, file_kind, bucket_name, object_key, content_type, size_bytes, temporary) values (?, 'OUTPUT_MARKDOWN', ?, ?, 'text/markdown', ?, false)",
                item.reportId(), storage.bucket(), outputKey, result.markdown().getBytes(StandardCharsets.UTF_8).length);
        int attempts = jdbc.queryForObject("select count(*) from report_attempts where report_id = ?", Integer.class, item.reportId());
        jdbc.update("update report_attempts set status = 'SUCCEEDED', finished_at = current_timestamp where report_id = ? and attempt_number = ?", item.reportId(), attempts);
        quotas.confirmForWorker(item.reservationId(), item.accountId());
        quotas.recordMetric(item.accountId(), new br.com.sres.usage.MetricRequest(item.reservationId(), result.model(),
                result.inputTokens() == null ? null : result.inputTokens().longValue(),
                result.outputTokens() == null ? null : result.outputTokens().longValue(), durationMs, attempts));
        jdbc.update("update reports set status = 'COMPLETED', updated_at = current_timestamp where id = ? and status = 'PROCESSING'", item.reportId());
        events.publishEvent(new ReportCompletedEvent(item.reportId(), item.accountId()));
    }

    void fail(WorkItem item, RuntimeException exception, long durationMs) {
        int attempt = jdbc.queryForObject("select count(*) from report_attempts where report_id = ?", Integer.class, item.reportId());
        String status = attempt >= MAX_ATTEMPTS ? "FAILED" : "PENDING";
        jdbc.update("update report_attempts set status = 'FAILED', error_message = ?, finished_at = current_timestamp where report_id = ? and attempt_number = ?",
                safeMessage(exception), item.reportId(), attempt);
        if ("FAILED".equals(status)) {
            quotas.releaseForWorker(item.reservationId(), item.accountId());
            jdbc.update("update reports set status = 'FAILED', updated_at = current_timestamp where id = ?", item.reportId());
            events.publishEvent(new ReportFailedEvent(item.reportId(), item.accountId()));
        } else {
            jdbc.update("update reports set status = 'PENDING', updated_at = current_timestamp where id = ?", item.reportId());
        }
    }

    private String prompt(WorkItem item) {
        String instructions = switch (item.type()) {
            case "EXECUTIVE_SUMMARY" -> "prompts/executive-summary-v1.txt";
            case "DETAILED_ANALYSIS" -> "prompts/detailed-analysis-v1.txt";
            case "STRUCTURED_EXTRACTION" -> "prompts/structured-extraction-v1.txt";
            default -> throw new IllegalArgumentException("Tipo de relatório inválido");
        };
        try (var stream = getClass().getClassLoader().getResourceAsStream(instructions)) {
            if (stream == null) throw new IllegalStateException("Prompt não encontrado: " + instructions);
            String template = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return template.replace("{{report_type}}", item.type())
                    .replace("{{description}}", item.description())
                    .replace("{{extracted_text}}", item.extractedText() == null ? "" : item.extractedText());
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao carregar prompt", exception);
        }
    }

    private static String safeMessage(Throwable exception) {
        String message = exception.getMessage();
        return message == null ? exception.getClass().getSimpleName() : message.substring(0, Math.min(message.length(), 500));
    }

    record WorkItem(UUID reportId, UUID accountId, String type, String description, String extractedText, UUID reservationId) { }
    public record ReportCompletedEvent(UUID reportId, UUID accountId) { }
    public record ReportFailedEvent(UUID reportId, UUID accountId) { }
}
