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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administration")
@SecurityRequirement(name = "bearerAuth")
public class UsageAdminController {
    private final QuotaService quotas;
    private final AccountService accounts;

    public UsageAdminController(QuotaService quotas, AccountService accounts) { this.quotas = quotas; this.accounts = accounts; }

    @PostMapping("/accounts/{id}/quota/adjust")
    @Operation(summary = "Ajustar quota da conta", description = "Aplica ajuste auditável na quota de uma conta. Requer ROLE_ADMIN.")
    public UsageSummary adjust(@PathVariable UUID id, @Valid @RequestBody QuotaAdjustmentRequest request,
                               @AuthenticationPrincipal Jwt actor) {
        return quotas.adjust(id, request.units(), actor.getSubject(), request.reason());
    }

    @PutMapping("/accounts/{id}/plan")
    @Operation(summary = "Alterar plano da conta", description = "Atribui plano e nova alocação conforme as regras administrativas. Requer ROLE_ADMIN.")
    public UsageSummary changePlan(@PathVariable UUID id, @Valid @RequestBody PlanAssignmentRequest request,
                                   @AuthenticationPrincipal Jwt actor) {
        return quotas.changePlan(id, request.planId(), actor.getSubject(), request.reason());
    }

    @PostMapping("/accounts/{id}/usage/metrics")
    @Operation(summary = "Registrar métrica de uso", description = "Registra tokens, duração, tentativas e custo estimado. Requer ROLE_ADMIN.")
    @ResponseStatus(HttpStatus.CREATED)
    public MetricResponse metric(@PathVariable UUID id, @Valid @RequestBody MetricRequest request) {
        return quotas.recordMetric(id, request);
    }

    @GetMapping("/accounts/{id}/usage")
    @Operation(summary = "Consultar quota de uma conta", description = "Consulta quota de uma conta para administração. Requer ROLE_ADMIN.")
    public UsageSummary accountUsage(@PathVariable UUID id) { return quotas.usageForAccount(id); }

    @GetMapping("/accounts/{id}/usage/history")
    @Operation(summary = "Consultar histórico de quota de uma conta", description = "Requer ROLE_ADMIN.")
    public List<LedgerResponse> accountHistory(@PathVariable UUID id) { return quotas.historyForAccount(id); }

    @GetMapping("/costs")
    @Operation(summary = "Consultar custos", description = "Consulta custos monetários estimados. Requer ROLE_ADMIN; nunca é uma operação de USER.")
    public List<MetricResponse> costs() { return quotas.costs(); }

    @PostMapping("/usage/renew")
    @Operation(summary = "Renovar quotas", description = "Executa a renovação administrativa das quotas elegíveis. Requer ROLE_ADMIN.")
    public String renew() { return "renewed=" + quotas.renewAll(); }
}
