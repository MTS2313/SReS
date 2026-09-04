package br.com.sres.usage;

import br.com.sres.accounts.AccountService;
import br.com.sres.plans.PlanAssignmentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class UsageAdminController {
    private final QuotaService quotas;
    private final AccountService accounts;

    public UsageAdminController(QuotaService quotas, AccountService accounts) { this.quotas = quotas; this.accounts = accounts; }

    @PostMapping("/accounts/{id}/quota/adjust")
    public UsageSummary adjust(@PathVariable UUID id, @Valid @RequestBody QuotaAdjustmentRequest request,
                               @AuthenticationPrincipal Jwt actor) {
        return quotas.adjust(id, request.units(), actor.getSubject(), request.reason());
    }

    @PutMapping("/accounts/{id}/plan")
    public UsageSummary changePlan(@PathVariable UUID id, @Valid @RequestBody PlanAssignmentRequest request,
                                   @AuthenticationPrincipal Jwt actor) {
        return quotas.changePlan(id, request.planId(), actor.getSubject(), request.reason());
    }

    @PostMapping("/accounts/{id}/usage/metrics")
    @ResponseStatus(HttpStatus.CREATED)
    public MetricResponse metric(@PathVariable UUID id, @Valid @RequestBody MetricRequest request) {
        return quotas.recordMetric(id, request);
    }

    @GetMapping("/accounts/{id}/usage")
    public UsageSummary accountUsage(@PathVariable UUID id) { return quotas.usageForAccount(id); }

    @GetMapping("/accounts/{id}/usage/history")
    public List<LedgerResponse> accountHistory(@PathVariable UUID id) { return quotas.historyForAccount(id); }

    @GetMapping("/costs")
    public List<MetricResponse> costs() { return quotas.costs(); }

    @PostMapping("/usage/renew")
    public String renew() { return "renewed=" + quotas.renewAll(); }
}
