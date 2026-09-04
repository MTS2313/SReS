package br.com.sres.telegram;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "sres.integrations.telegram.enabled", havingValue = "false", matchIfMissing = true)
public class TelegramDisabledGateway implements TelegramGateway {
    @Override public void sendMessage(long chatId, String message) { }
    @Override public void sendResult(long chatId, String summary, byte[] markdown) { }
    @Override public byte[] download(TelegramUpdate.Document document) { throw new IllegalStateException("Telegram desabilitado"); }
}
