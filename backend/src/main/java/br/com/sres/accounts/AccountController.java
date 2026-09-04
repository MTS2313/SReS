package br.com.sres.accounts;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Account")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {
    private final AccountService accounts;

    public AccountController(AccountService accounts) { this.accounts = accounts; }

    @GetMapping("/me")
    @Operation(summary = "Consultar minha conta", description = "Deriva a identidade exclusivamente do subject do JWT autenticado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Conta autenticada"),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária", content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = "#/components/schemas/ProblemDetails")))})
    public AccountResponse me(@AuthenticationPrincipal Jwt jwt) { return accounts.me(jwt); }
}
