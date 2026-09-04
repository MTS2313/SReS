package br.com.sres.telegram;

import br.com.sres.processing.ReportProcessingWorker;
import br.com.sres.storage.StorageService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class TelegramDeliveryService {
    private final JdbcTemplate jdbc;
    private final TelegramGateway gateway;
    private final StorageService storage;

    public TelegramDeliveryService(JdbcTemplate jdbc, TelegramGateway gateway, StorageService storage) {
        this.jdbc = jdbc; this.gateway = gateway; this.storage = storage;
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCompleted(ReportProcessingWorker.ReportCompletedEvent event) {
        List<Destination> destinations = jdbc.query("select telegram_user_id, chat_id from telegram_links where account_id = ?",
                (rs, row) -> new Destination(rs.getLong("telegram_user_id"), rs.getLong("chat_id")), event.accountId());
        for (Destination destination : destinations) {
            UUID deliveryId = UUID.randomUUID();
            int inserted = jdbc.update("insert into telegram_deliveries (id, report_id, account_id, telegram_user_id, chat_id, status, attempt_count, next_attempt_at) values (?, ?, ?, ?, ?, 'PENDING', 0, current_timestamp) on conflict (report_id) do nothing",
                    deliveryId, event.reportId(), event.accountId(), destination.telegramUserId(), destination.chatId());
            if (inserted == 1) deliver(deliveryId);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFailed(ReportProcessingWorker.ReportFailedEvent event) {
        jdbc.queryForList("select chat_id from telegram_links where account_id = ?", event.accountId())
                .forEach(row -> gateway.sendMessage(((Number) row.get("chat_id")).longValue(), "Não foi possível processar seu relatório."));
    }

    @Transactional
    public void deliverDue() {
        List<UUID> ids = jdbc.query("select id from telegram_deliveries where status = 'PENDING' and next_attempt_at <= current_timestamp order by next_attempt_at for update skip locked",
                (rs, row) -> rs.getObject("id", UUID.class));
        ids.forEach(this::deliver);
    }

    private void deliver(UUID deliveryId) {
        List<Delivery> rows = jdbc.query("select d.id, d.chat_id, d.report_id, d.attempt_count, r.description, f.object_key from telegram_deliveries d join reports r on r.id = d.report_id left join report_files f on f.report_id = r.id and f.file_kind = 'OUTPUT_MARKDOWN' where d.id = ? and d.status = 'PENDING'", (rs, row) ->
                new Delivery(rs.getObject("id", UUID.class), rs.getLong("chat_id"), rs.getObject("report_id", UUID.class), rs.getInt("attempt_count"), rs.getString("description"), rs.getString("object_key")), deliveryId);
        if (rows.isEmpty()) return;
        Delivery delivery = rows.getFirst();
        int attempt = delivery.attempts() + 1;
        jdbc.update("update telegram_deliveries set attempt_count = ?, updated_at = current_timestamp where id = ?", attempt, delivery.id());
        try {
            if (delivery.objectKey() == null) throw new IllegalStateException("Resultado Markdown não encontrado");
            byte[] markdown;
            try (var object = storage.get(delivery.objectKey())) { markdown = object.readAllBytes(); }
            gateway.sendResult(delivery.chatId(), "Relatório concluído: " + delivery.description(), markdown);
            jdbc.update("insert into telegram_delivery_attempts (delivery_id, attempt_number, status) values (?, ?, 'SUCCEEDED')", delivery.id(), attempt);
            jdbc.update("update telegram_deliveries set status = 'SENT', sent_at = current_timestamp, next_attempt_at = null, last_error = null, updated_at = current_timestamp where id = ?", delivery.id());
        } catch (IOException | RuntimeException exception) {
            jdbc.update("insert into telegram_delivery_attempts (delivery_id, attempt_number, status, error_message) values (?, ?, 'FAILED', ?)", delivery.id(), attempt, safe(exception));
            if (attempt >= 3) jdbc.update("update telegram_deliveries set status = 'FAILED', next_attempt_at = null, last_error = ?, updated_at = current_timestamp where id = ?", safe(exception), delivery.id());
            else jdbc.update("update telegram_deliveries set next_attempt_at = current_timestamp + (? * interval '1 minute'), last_error = ?, updated_at = current_timestamp where id = ?", attempt == 1 ? 1 : 5, safe(exception), delivery.id());
        }
    }

    private static String safe(Exception exception) {
        String message = exception.getMessage();
        return message == null ? exception.getClass().getSimpleName() : message.substring(0, Math.min(message.length(), 500));
    }

    private record Destination(long telegramUserId, long chatId) { }
    private record Delivery(UUID id, long chatId, UUID reportId, int attempts, String description, String objectKey) { }
}
