package br.com.sres.telegram;

import br.com.sres.accounts.AccountEntity;
import br.com.sres.accounts.AccountRepository;
import br.com.sres.accounts.AccountStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class TelegramLinkService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AccountRepository accounts;
    private final JdbcTemplate jdbc;

    public TelegramLinkService(AccountRepository accounts, JdbcTemplate jdbc) {
        this.accounts = accounts;
        this.jdbc = jdbc;
    }

    @Transactional
    public TelegramLinkResponse generate(UUID accountId) {
        AccountEntity account = accounts.findById(accountId).orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
        if (account.getStatus() == AccountStatus.BLOCKED) throw new IllegalStateException("Conta bloqueada não pode gerar vínculo");
        if (jdbc.queryForObject("select count(*) from telegram_links where account_id = ?", Integer.class, accountId) > 0)
            throw new IllegalStateException("Conta já possui vínculo Telegram");
        String code = randomCode();
        Instant expires = Instant.now().plusSeconds(600);
        UUID codeId = jdbc.queryForObject("insert into telegram_link_codes (account_id, code_hash, expires_at) values (?, ?, ?) returning id", UUID.class,
                accountId, hash(code), java.sql.Timestamp.from(expires));
        return new TelegramLinkResponse(codeId, code, expires);
    }

    @Transactional
    public boolean consume(String code, long telegramUserId, long chatId) {
        var rows = jdbc.query("select id, account_id, expires_at from telegram_link_codes where code_hash = ? and used_at is null for update",
                (rs, row) -> new CodeRow(rs.getObject("id", UUID.class), rs.getObject("account_id", UUID.class), rs.getTimestamp("expires_at").toInstant()), hash(code));
        if (rows.isEmpty() || !rows.getFirst().expiresAt().isAfter(Instant.now())) return false;
        CodeRow linkCode = rows.getFirst();
        AccountEntity account = accounts.findById(linkCode.accountId()).orElseThrow();
        if (account.getStatus() == AccountStatus.BLOCKED) return false;
        if (jdbc.queryForObject("select count(*) from telegram_links where account_id = ? or telegram_user_id = ?", Integer.class, linkCode.accountId(), telegramUserId) > 0) return false;
        jdbc.update("insert into telegram_links (account_id, telegram_user_id, chat_id) values (?, ?, ?)", linkCode.accountId(), telegramUserId, chatId);
        jdbc.update("update telegram_link_codes set used_at = current_timestamp where id = ?", linkCode.id());
        return true;
    }

    public UUID accountForTelegram(long telegramUserId) {
        return jdbc.queryForObject("select account_id from telegram_links where telegram_user_id = ?", UUID.class, telegramUserId);
    }

    public String hashForTest(String code) { return hash(code); }

    private static String randomCode() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : digest) result.append(String.format("%02x", value));
            return result.toString();
        } catch (Exception exception) { throw new IllegalStateException(exception); }
    }

    private record CodeRow(UUID id, UUID accountId, Instant expiresAt) { }
}
