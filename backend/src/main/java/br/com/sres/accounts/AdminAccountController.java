package br.com.sres.accounts;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/admin/accounts")
@Tag(name = "Administration")
@SecurityRequirement(name = "bearerAuth")
public class AdminAccountController {
    private final AccountService accounts;

    public AdminAccountController(AccountService accounts) { this.accounts = accounts; }

    @GetMapping
    @Operation(summary = "Listar contas", description = "Lista contas locais provisionadas. Requer ROLE_ADMIN.")
    public List<AccountResponse> all() { return accounts.all(); }

    @PostMapping("/{id}/block")
    @Operation(summary = "Bloquear conta", description = "Bloqueia novos consumos da conta sem remover seu histórico. Requer ROLE_ADMIN.")
    public AccountResponse block(@PathVariable UUID id, @AuthenticationPrincipal Jwt actor) {
        return accounts.block(id, actor.getSubject(), null);
    }

    @PostMapping("/{id}/unblock")
    @Operation(summary = "Desbloquear conta", description = "Reabilita novos consumos da conta. Requer ROLE_ADMIN.")
    public AccountResponse unblock(@PathVariable UUID id, @AuthenticationPrincipal Jwt actor) {
        return accounts.unblock(id, actor.getSubject(), null);
    }

}
