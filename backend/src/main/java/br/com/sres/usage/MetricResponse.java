package br.com.sres.usage;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MetricResponse", description = "Métrica e custo estimado de processamento; disponível em operações administrativas.")
public record MetricResponse(UUID id, UUID accountId, String model, long inputTokens,
                             long outputTokens, long totalTokens, long durationMs,
                             int attempts, BigDecimal estimatedCost, Instant createdAt) { }
