package br.com.sres.plans;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/admin/plans")
@Tag(name = "Administration")
@SecurityRequirement(name = "bearerAuth")
public class PlanController {
    private final PlanService plans;

    public PlanController(PlanService plans) { this.plans = plans; }

    @GetMapping
    @Operation(summary = "Listar planos", description = "Lista planos disponíveis para administração. Requer ROLE_ADMIN.")
    public List<PlanResponse> all() { return plans.all(); }

    @PostMapping
    @Operation(summary = "Criar plano", description = "Cria um plano de relatórios. Requer ROLE_ADMIN.")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanResponse create(@Valid @RequestBody PlanRequest request, @AuthenticationPrincipal Jwt actor) {
        return plans.create(request, actor.getSubject());
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Alterar plano", description = "Altera os dados permitidos de um plano. Requer ROLE_ADMIN.")
    public PlanResponse update(@PathVariable UUID id, @Valid @RequestBody PlanUpdateRequest request) {
        return plans.update(id, request);
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Ativar plano", description = "Ativa um plano. Requer ROLE_ADMIN.")
    public PlanResponse activate(@PathVariable UUID id) { return plans.activate(id); }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Inativar plano", description = "Inativa um plano sem migrar contas automaticamente. Requer ROLE_ADMIN.")
    public PlanResponse deactivate(@PathVariable UUID id) { return plans.deactivate(id); }

    @PostMapping("/{id}/default")
    @Operation(summary = "Definir plano padrão", description = "Define o plano padrão para novas contas. Requer ROLE_ADMIN.")
    public PlanResponse makeDefault(@PathVariable UUID id) { return plans.makeDefault(id); }
}
