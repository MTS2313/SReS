package br.com.sres;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationIntegrationTest {
    static {
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
        registry.add("sres.integrations.ollama.enabled", () -> "false");
        registry.add("sres.integrations.telegram.enabled", () -> "false");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void publishesUsableOpenApiDocument() throws Exception {
        JsonNode document = document();

        assertThat(document.path("info").path("title").asText()).isEqualTo("SReS API");
        assertThat(document.path("info").path("description").asText()).contains("Keycloak");
        assertThat(document.path("components").path("securitySchemes").path("bearerAuth").path("type").asText())
                .isEqualTo("http");
        assertThat(document.path("components").path("securitySchemes").path("bearerAuth").path("scheme").asText())
                .isEqualTo("bearer");
        assertThat(document.path("components").path("securitySchemes").path("bearerAuth").path("bearerFormat").asText())
                .isEqualTo("JWT");
    }

    @Test
    void containsOnlyRelevantTagsAndEssentialSchemas() throws Exception {
        JsonNode document = document();
        var tags = StreamSupport.stream(document.path("tags").spliterator(), false)
                .map(node -> node.path("name").asText()).toList();

        assertThat(tags).contains("Account", "Plans", "Usage", "Reports", "Telegram", "Administration");
        assertThat(document.path("components").path("schemas").has("AccountResponse")).isTrue();
        assertThat(document.path("components").path("schemas").has("PlanResponse")).isTrue();
        assertThat(document.path("components").path("schemas").has("UsageSummary")).isTrue();
        assertThat(document.path("components").path("schemas").has("ReportResponse")).isTrue();
        assertThat(document.path("components").path("schemas").has("TelegramLinkResponse")).isTrue();
        assertThat(document.path("components").path("schemas").has("ProblemDetails")).isTrue();
    }

    @Test
    void documentsCriticalOperationsAndAdministrativeSecurity() throws Exception {
        JsonNode paths = document().path("paths");
        assertThat(paths.has("/api/v1/me")).isTrue();
        assertThat(paths.has("/api/v1/reports")).isTrue();
        assertThat(paths.has("/api/v1/reports/{id}/input")).isTrue();
        assertThat(paths.has("/api/v1/reports/{id}/output")).isTrue();
        assertThat(paths.has("/api/v1/usage")).isTrue();
        assertThat(paths.has("/api/v1/me/telegram-link")).isTrue();
        assertThat(paths.has("/api/v1/admin/accounts")).isTrue();
        assertThat(paths.has("/api/v1/admin/plans")).isTrue();
        assertThat(paths.has("/api/v1/admin/costs")).isTrue();
        assertThat(paths.path("/api/v1/reports").path("post").path("responses").has("202")).isTrue();
        assertThat(paths.path("/api/v1/reports").path("post").path("requestBody").path("content")
                .has("multipart/form-data")).isTrue();
        JsonNode multipartSchema = paths.path("/api/v1/reports").path("post").path("requestBody").path("content")
                .path("multipart/form-data").path("schema").path("properties");
        if (multipartSchema.isMissingNode()) {
            multipartSchema = document().path("components").path("schemas").path("ReportMultipartRequest").path("properties");
        }
        assertThat(multipartSchema.has("type")).isTrue();
        assertThat(multipartSchema.has("description")).isTrue();
        assertThat(multipartSchema.has("file")).isTrue();
        assertThat(paths.path("/api/v1/reports").path("post").path("parameters").toString())
                .contains("Idempotency-Key");
        assertThat(paths.path("/api/v1/reports/{id}/input").path("get").path("responses").has("200")).isTrue();
        assertThat(paths.path("/api/v1/reports/{id}/output").path("get").path("responses").has("200")).isTrue();
        assertThat(paths.path("/api/v1/admin/costs").path("get").path("security").toString())
                .contains("bearerAuth");
    }

    @Test
    void exposesSwaggerUiWhenEnabled() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    private JsonNode document() throws Exception {
        var result = mockMvc.perform(get("/v3/api-docs")).andReturn();
        if (result.getResponse().getStatus() != 200) {
            throw new AssertionError("OpenAPI endpoint failed", result.getResolvedException());
        }
        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    @TestConfiguration
    static class TestSecurityConfiguration {
        @Bean
        @Primary
        JwtDecoder jwtDecoder() {
            return token -> {
                throw new BadJwtException("invalid token");
            };
        }
    }
}
