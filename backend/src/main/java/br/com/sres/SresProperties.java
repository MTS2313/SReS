package br.com.sres;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sres.integrations")
public record SresProperties(Integration ollama, Integration telegram) {
    public record Integration(boolean enabled) { }
}
