package br.com.sres.telegram;

import java.util.UUID;

public record TelegramConversation(long telegramUserId, UUID accountId, long chatId, String state,
                                   String reportType, String description) { }
