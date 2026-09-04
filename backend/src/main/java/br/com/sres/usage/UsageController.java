package br.com.sres.usage;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class UsageController {
    private final QuotaService quotas;

    public UsageController(QuotaService quotas) { this.quotas = quotas; }

    @GetMapping("/usage")
    public UsageSummary usage(@AuthenticationPrincipal Jwt jwt) { return quotas.usage(jwt); }

    @GetMapping("/usage/history")
    public List<LedgerResponse> history(@AuthenticationPrincipal Jwt jwt) { return quotas.history(jwt); }

    @PostMapping("/usage/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserve(@AuthenticationPrincipal Jwt jwt, @RequestHeader("Idempotency-Key") String key) {
        return quotas.reserve(jwt, key);
    }

    @PostMapping("/usage/reservations/{id}/confirm")
    public ReservationResponse confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) { return quotas.confirm(jwt, id); }

    @PostMapping("/usage/reservations/{id}/release")
    public ReservationResponse release(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) { return quotas.release(jwt, id); }
}
