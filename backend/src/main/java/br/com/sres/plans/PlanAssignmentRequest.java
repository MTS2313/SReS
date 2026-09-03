package br.com.sres.plans;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PlanAssignmentRequest(@NotNull UUID planId, String reason) { }
