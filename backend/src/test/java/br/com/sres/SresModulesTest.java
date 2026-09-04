package br.com.sres;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

class SresModulesTest {
    @Test
    void exposesTheMvpApplicationModules() {
        var modules = ApplicationModules.of(SresApplication.class);
        assertThat(modules.stream().map(module -> module.getName()).toList())
                .containsExactlyInAnyOrder("accounts", "plans", "usage", "reports", "storage", "ollama", "telegram", "administration", "processing", "maintenance");
        modules.verify();
    }
}
