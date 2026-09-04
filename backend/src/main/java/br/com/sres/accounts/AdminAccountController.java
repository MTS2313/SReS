package br.com.sres.accounts;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/accounts")
public class AdminAccountController {
    private final AccountService accounts;

    public AdminAccountController(AccountService accounts) { this.accounts = accounts; }

    @GetMapping
    public List<AccountResponse> all() { return accounts.all(); }

    @PostMapping("/{id}/block")
    public AccountResponse block(@PathVariable UUID id, @AuthenticationPrincipal Jwt actor) {
        return accounts.block(id, actor.getSubject(), null);
    }

    @PostMapping("/{id}/unblock")
    public AccountResponse unblock(@PathVariable UUID id, @AuthenticationPrincipal Jwt actor) {
        return accounts.unblock(id, actor.getSubject(), null);
    }

}
