package br.com.sres.telegram;

import java.time.Instant;
import java.util.UUID;

public record TelegramLinkResponse(UUID id, String code, Instant expiresAt) { }
