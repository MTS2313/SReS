package br.com.sres.usage;

import jakarta.validation.constraints.Min;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MetricRequest", description = "Métrica de processamento registrada por administração.")
public record MetricRequest(UUID reservationId, String model, @Min(0) Long inputTokens,
                            @Min(0) Long outputTokens, @Min(0) Long durationMs, @Min(1) Integer attempts) { }
