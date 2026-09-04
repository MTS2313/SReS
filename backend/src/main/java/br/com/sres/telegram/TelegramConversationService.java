package br.com.sres.telegram;

import br.com.sres.reports.ReportService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TelegramConversationService {
    private final JdbcTemplate jdbc;
    private final TelegramLinkService links;
    private final TelegramGateway gateway;
    private final ReportService reports;

    public TelegramConversationService(JdbcTemplate jdbc, TelegramLinkService links, TelegramGateway gateway, ReportService reports) {
        this.jdbc = jdbc; this.links = links; this.gateway = gateway; this.reports = reports;
    }

    public boolean process(TelegramUpdate update) {
        if (!register(update.updateId())) return false;
        java.util.UUID accountId = account(update.telegramUserId(), update.text(), update.chatId());
        if (accountId == null) return true;
        if ("/start".equalsIgnoreCase(update.text())) {
            save(new TelegramConversation(update.telegramUserId(), accountId, update.chatId(), "TYPE_SELECTION", null, null));
            gateway.sendMessage(update.chatId(), "Escolha o tipo: EXECUTIVE_SUMMARY, DETAILED_ANALYSIS ou STRUCTURED_EXTRACTION.");
            return true;
        }
        if ("/cancel".equalsIgnoreCase(update.text())) {
            jdbc.update("delete from telegram_conversations where telegram_user_id = ?", update.telegramUserId());
            gateway.sendMessage(update.chatId(), "Conversa cancelada.");
            return true;
        }
        Optional<TelegramConversation> current = load(update.telegramUserId());
        if (current.isEmpty()) {
            save(new TelegramConversation(update.telegramUserId(), accountId, update.chatId(), "TYPE_SELECTION", null, null));
            gateway.sendMessage(update.chatId(), "Escolha o tipo: EXECUTIVE_SUMMARY, DETAILED_ANALYSIS ou STRUCTURED_EXTRACTION.");
            return true;
        }
        TelegramConversation conversation = current.get();
        switch (conversation.state()) {
            case "TYPE_SELECTION" -> selectType(conversation, update.text());
            case "DESCRIPTION" -> save(new TelegramConversation(conversation.telegramUserId(), conversation.accountId(), conversation.chatId(), "PDF_OPTIONAL", conversation.reportType(), update.text()));
            case "PDF_OPTIONAL" -> submit(conversation, update);
            default -> jdbc.update("delete from telegram_conversations where telegram_user_id = ?", update.telegramUserId());
        }
        return true;
    }

    public Optional<TelegramConversation> load(long telegramUserId) {
        List<TelegramConversation> rows = jdbc.query("select telegram_user_id, account_id, chat_id, state, report_type, description from telegram_conversations where telegram_user_id = ? and updated_at > current_timestamp - interval '30 minutes'",
                (rs, row) -> new TelegramConversation(rs.getLong("telegram_user_id"), rs.getObject("account_id", java.util.UUID.class), rs.getLong("chat_id"), rs.getString("state"), rs.getString("report_type"), rs.getString("description")), telegramUserId);
        if (rows.isEmpty()) {
            jdbc.update("delete from telegram_conversations where telegram_user_id = ?", telegramUserId);
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }

    private java.util.UUID account(long telegramUserId, String text, long chatId) {
        try { return links.accountForTelegram(telegramUserId); }
        catch (org.springframework.dao.EmptyResultDataAccessException absent) {
            if (text != null && links.consume(text.trim(), telegramUserId, chatId)) {
                gateway.sendMessage(chatId, "Telegram vinculado com sucesso. Envie /start para solicitar um relatório.");
                return links.accountForTelegram(telegramUserId);
            }
            gateway.sendMessage(chatId, "Telegram não vinculado. Use o código gerado pela API.");
            return null;
        }
    }

    private void selectType(TelegramConversation conversation, String type) {
        if (!List.of("EXECUTIVE_SUMMARY", "DETAILED_ANALYSIS", "STRUCTURED_EXTRACTION").contains(type)) {
            gateway.sendMessage(conversation.chatId(), "Tipo inválido. Escolha um dos tipos disponíveis."); return;
        }
        save(new TelegramConversation(conversation.telegramUserId(), conversation.accountId(), conversation.chatId(), "DESCRIPTION", type, null));
        gateway.sendMessage(conversation.chatId(), "Envie a descrição do relatório.");
    }

    private void submit(TelegramConversation conversation, TelegramUpdate update) {
        if (!"/skip".equalsIgnoreCase(update.text()) && update.document() == null) {
            gateway.sendMessage(conversation.chatId(), "Envie um PDF ou use /skip."); return;
        }
        try {
            org.springframework.web.multipart.MultipartFile file = null;
            if (update.document() != null) {
                var document = update.document();
                file = new ByteArrayMultipartFile(gateway.download(document), document.fileName(), document.contentType());
            }
            reports.createForAccount(conversation.accountId(), "telegram:" + update.telegramUserId(), conversation.reportType(), conversation.description(), file,
                    "telegram-update:" + update.updateId(), "TELEGRAM");
            jdbc.update("delete from telegram_conversations where telegram_user_id = ?", conversation.telegramUserId());
            gateway.sendMessage(conversation.chatId(), "Relatório recebido e aguardando processamento.");
        } catch (RuntimeException exception) {
            gateway.sendMessage(conversation.chatId(), "Não foi possível criar o relatório: " + exception.getMessage());
        }
    }

    private boolean register(long updateId) {
        try { return jdbc.update("insert into telegram_processed_updates (update_id) values (?) on conflict (update_id) do nothing", updateId) == 1; }
        catch (RuntimeException exception) { return false; }
    }

    private void save(TelegramConversation conversation) {
        jdbc.update("insert into telegram_conversations (telegram_user_id, account_id, chat_id, state, report_type, description) values (?, ?, ?, ?, ?, ?) on conflict (telegram_user_id) do update set account_id = excluded.account_id, chat_id = excluded.chat_id, state = excluded.state, report_type = excluded.report_type, description = excluded.description, updated_at = current_timestamp",
                conversation.telegramUserId(), conversation.accountId(), conversation.chatId(), conversation.state(), conversation.reportType(), conversation.description());
    }
}
