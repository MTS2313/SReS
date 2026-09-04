package br.com.sres.usage;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Usage")
@SecurityRequirement(name = "bearerAuth")
public class UsageController {
    private final QuotaService quotas;

    public UsageController(QuotaService quotas) { this.quotas = quotas; }

    @GetMapping("/usage")
    @Operation(summary = "Consultar minha quota", description = "Retorna o saldo e a renovação da quota da conta autenticada.")
    public UsageSummary usage(@AuthenticationPrincipal Jwt jwt) { return quotas.usage(jwt); }

    @GetMapping("/usage/history")
    @Operation(summary = "Consultar meu histórico de quota")
    public List<LedgerResponse> history(@AuthenticationPrincipal Jwt jwt) { return quotas.history(jwt); }

    @PostMapping("/usage/reservations")
    @Operation(summary = "Reservar unidade de quota", description = "Cria uma reserva idempotente de uma unidade de quota para uso interno autorizado.")
    @ApiResponse(responseCode = "201", description = "Reserva criada")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserve(@AuthenticationPrincipal Jwt jwt, @Parameter(description = "Chave obrigatória de idempotência da reserva", example = "reservation-key-2026-001") @RequestHeader("Idempotency-Key") String key) {
        return quotas.reserve(jwt, key);
    }

    @PostMapping("/usage/reservations/{id}/confirm")
    @Operation(summary = "Confirmar reserva")
    public ReservationResponse confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) { return quotas.confirm(jwt, id); }

    @PostMapping("/usage/reservations/{id}/release")
    @Operation(summary = "Liberar reserva")
    public ReservationResponse release(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) { return quotas.release(jwt, id); }
}
