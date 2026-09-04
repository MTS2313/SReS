package br.com.sres.accounts;

import br.com.sres.plans.PlanResponse;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AccountResponse", description = "Conta local associada à identidade autenticada.", example = "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"subject\":\"subject-ficticio\",\"username\":\"usuario-demo\",\"email\":\"usuario@example.test\",\"status\":\"ACTIVE\",\"plan\":{\"name\":\"Plano Inicial\",\"weeklyLimit\":10,\"active\":true,\"isDefault\":true}}")
public record AccountResponse(UUID id, String subject, String username, String email, AccountStatus status, PlanResponse plan) { }
