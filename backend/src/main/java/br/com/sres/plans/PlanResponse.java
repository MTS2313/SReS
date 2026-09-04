package br.com.sres.plans;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PlanResponse", description = "Plano de relatórios.", example = "{\"id\":\"00000000-0000-0000-0000-000000000010\",\"name\":\"Plano Inicial\",\"weeklyLimit\":10,\"active\":true,\"isDefault\":true}")
public record PlanResponse(UUID id, String name, int weeklyLimit, boolean active, boolean isDefault) { }
