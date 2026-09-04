package br.com.sres.usage;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MetricResponse(UUID id, UUID accountId, String model, long inputTokens,
                             long outputTokens, long totalTokens, long durationMs,
                             int attempts, BigDecimal estimatedCost, Instant createdAt) { }
