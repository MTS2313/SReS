package br.com.sres.plans;

import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PlanUpdateRequest", description = "Campos alteráveis de um plano.")
public record PlanUpdateRequest(String name, @Min(1) Integer weeklyLimit) { }
