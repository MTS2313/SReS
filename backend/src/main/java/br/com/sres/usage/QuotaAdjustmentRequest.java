package br.com.sres.usage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuotaAdjustmentRequest(@NotNull Integer units, @NotBlank String reason) { }
