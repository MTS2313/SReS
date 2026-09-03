package br.com.sres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration",
        "spring.jpa.hibernate.ddl-auto=validate",
        "sres.integrations.ollama.enabled=false",
        "sres.integrations.telegram.enabled=false"
})
class SresContextIntegrationTest {
    static {
        // The local Docker daemon requires API 1.44; docker-java otherwise defaults to 1.32.
        System.setProperty("api.version", "1.44");
    }

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.6")
            .withDatabaseName("sres")
            .withUsername("sres_dev")
            .withPassword("change-me-development-only");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    SresProperties properties;

    @Value("${spring.jpa.hibernate.ddl-auto}")
    String ddlAuto;

    @Test
    void startsWithPostgresFlywayAndJpaValidationAndOptionalIntegrationsOff() {
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version = '1' and success = true", Integer.class))
                .isEqualTo(1);
        assertThat(ddlAuto).isEqualTo("validate");
        assertThat(properties.ollama().enabled()).isFalse();
        assertThat(properties.telegram().enabled()).isFalse();
    }
}
