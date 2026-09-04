package br.com.sres.telegram;

import java.util.List;

public interface TelegramGateway {
    void sendMessage(long chatId, String message);
    void sendResult(long chatId, String summary, byte[] markdown);
    byte[] download(TelegramUpdate.Document document);
    default List<TelegramUpdate> poll() { return List.of(); }
}
