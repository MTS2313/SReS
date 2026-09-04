package br.com.sres.telegram;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "sres.integrations.telegram.enabled", havingValue = "true")
public class TelegramLongPolling {
    private final TelegramGateway gateway;
    private final TelegramConversationService conversations;

    public TelegramLongPolling(TelegramGateway gateway, TelegramConversationService conversations) { this.gateway = gateway; this.conversations = conversations; }

    @Scheduled(fixedDelayString = "${sres.telegram.poll-interval-ms:1000}")
    public void poll() {
        for (TelegramUpdate update : gateway.poll()) conversations.process(update);
    }
}
