package br.com.sres.usage;

import jakarta.validation.constraints.Min;

import java.util.UUID;

public record MetricRequest(UUID reservationId, String model, @Min(0) Long inputTokens,
                            @Min(0) Long outputTokens, @Min(0) Long durationMs, @Min(1) Integer attempts) { }
