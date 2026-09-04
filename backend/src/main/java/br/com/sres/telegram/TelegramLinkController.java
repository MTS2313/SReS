package br.com.sres.telegram;

import br.com.sres.accounts.AccountRepository;
import br.com.sres.accounts.AccountService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/telegram-link")
public class TelegramLinkController {
    private final AccountService accounts;
    private final AccountRepository repository;
    private final TelegramLinkService links;

    public TelegramLinkController(AccountService accounts, AccountRepository repository, TelegramLinkService links) {
        this.accounts = accounts; this.repository = repository; this.links = links;
    }

    @PostMapping
    public TelegramLinkResponse generate(@AuthenticationPrincipal Jwt jwt) {
        accounts.me(jwt);
        return links.generate(repository.findBySubject(jwt.getSubject()).orElseThrow().getId());
    }
}
