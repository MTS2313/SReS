package br.com.sres.plans;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PlanRequest", description = "Dados para criação de plano administrativo.", example = "{\"name\":\"Plano Profissional\",\"weeklyLimit\":25,\"active\":true}")
public record PlanRequest(@NotBlank String name, @Min(1) int weeklyLimit, Boolean active) { }
