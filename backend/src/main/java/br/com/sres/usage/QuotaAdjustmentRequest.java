package br.com.sres.usage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "QuotaAdjustmentRequest", description = "Ajuste administrativo auditável da quota.")
public record QuotaAdjustmentRequest(@NotNull Integer units, @NotBlank String reason) { }
