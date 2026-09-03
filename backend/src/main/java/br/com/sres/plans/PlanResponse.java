package br.com.sres.plans;

import java.util.UUID;

public record PlanResponse(UUID id, String name, int weeklyLimit, boolean active, boolean isDefault) { }
