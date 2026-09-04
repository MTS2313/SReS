package br.com.sres.telegram;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "sres.integrations.telegram.enabled", havingValue = "true")
class TelegramDeliveryScheduler {
    private final TelegramDeliveryService deliveries;

    TelegramDeliveryScheduler(TelegramDeliveryService deliveries) { this.deliveries = deliveries; }

    @Scheduled(fixedDelayString = "${sres.telegram.delivery-interval-ms:1000}")
    void deliverDue() { deliveries.deliverDue(); }
}
