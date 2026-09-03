package br.com.sres.plans;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PlanRequest(@NotBlank String name, @Min(1) int weeklyLimit, Boolean active) { }
