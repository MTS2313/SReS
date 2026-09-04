package br.com.sres.telegram;

import java.time.Instant;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TelegramLinkResponse", description = "Código temporário para vínculo com Telegram.", example = "{\"id\":\"00000000-0000-0000-0000-000000000030\",\"code\":\"123456789012\",\"expiresAt\":\"2026-09-04T12:10:00Z\"}")
public record TelegramLinkResponse(UUID id, String code, Instant expiresAt) { }
