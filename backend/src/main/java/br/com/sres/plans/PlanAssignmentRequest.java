package br.com.sres.plans;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PlanAssignmentRequest", description = "Plano e motivo da atribuição administrativa.")
public record PlanAssignmentRequest(@NotNull UUID planId, String reason) { }
