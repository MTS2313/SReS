package br.com.sres.telegram;

public record TelegramUpdate(long updateId, long telegramUserId, long chatId, String text, Document document) {
    public record Document(String fileId, String fileName, String contentType, long size, byte[] content) { }
}
