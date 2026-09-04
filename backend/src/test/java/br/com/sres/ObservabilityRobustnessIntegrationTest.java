package br.com.sres;

import br.com.sres.maintenance.OperationalMaintenanceService;
import br.com.sres.storage.StorageService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ObservabilityRobustnessIntegrationTest {
    static { System.setProperty("api.version", "1.44"); }

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.6")
            .withDatabaseName("sres").withUsername("sres_dev").withPassword("change-me-development-only");
    @Container
    static final GenericContainer<?> minio = new GenericContainer<>("minio/minio:RELEASE.2025-09-07T16-13-09Z")
            .withCommand("server /data --console-address :9001")
            .withEnv("MINIO_ROOT_USER", "minio_dev")
            .withEnv("MINIO_ROOT_PASSWORD", "change-me-development-only")
            .withExposedPorts(9000);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        if (!postgres.isRunning()) postgres.start();
        if (!minio.isRunning()) minio.start();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("sres.storage.endpoint", () -> "http://" + minio.getHost() + ":" + minio.getMappedPort(9000));
        registry.add("sres.storage.bucket", () -> "sres-observability-test");
        registry.add("sres.storage.access-key", () -> "minio_dev");
        registry.add("sres.storage.secret-key", () -> "change-me-development-only");
        registry.add("sres.processing.enabled", () -> "false");
        registry.add("sres.integrations.telegram.enabled", () -> "false");
    }

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired OperationalMaintenanceService maintenance;
    @Autowired StorageService storage;

    @BeforeEach
    void clean() {
        jdbc.update("delete from report_idempotency");
        jdbc.update("delete from telegram_conversations");
        jdbc.update("delete from reports");
        jdbc.update("delete from quota_reservations");
        jdbc.update("delete from quota_allocations");
        jdbc.update("delete from accounts");
    }

    @Test
    void errorsUseProblemDetailsAndCorrelationId() throws Exception {
        mockMvc.perform(get("/api/v1/reports/not-a-uuid")
                        .header("X-Correlation-ID", "corr-test-001")
                        .with(jwt().jwt(token -> token.subject("observability-user"))))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Correlation-ID", "corr-test-001"))
                .andExpect(result -> assertThat(result.getResponse().getContentType()).contains("application/problem+json"))
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertThat(JsonPath.<Integer>read(body, "$.status")).isEqualTo(400);
                    assertThat(JsonPath.<String>read(body, "$.correlationId")).isEqualTo("corr-test-001");
                    assertThat(body).doesNotContain("Exception", "SQL", "stack");
                });
    }

    @Test
    void authenticationAndAuthorizationErrorsAlsoCarryCorrelationId() throws Exception {
        mockMvc.perform(get("/api/v1/me").header("X-Correlation-ID", "corr-auth-001"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Correlation-ID", "corr-auth-001"));
        mockMvc.perform(get("/api/v1/admin/costs").with(jwt().jwt(token -> token.subject("user-001"))))
                .andExpect(status().isForbidden())
                .andExpect(header().exists("X-Correlation-ID"));
    }

    @Test
    void actuatorExposesOnlyHealthAndInfo() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/beans")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void adminReportsAndAuditAreProtectedAndAvailable() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports").with(jwt().jwt(token -> token.subject("user-admin-surface"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/reports").with(jwt().jwt(token -> token.subject("admin-surface"))
                        .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/audit").with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void maintenanceRemovesOnlyExpiredIdempotencyAndConversations() {
        UUID account = accountId("maintenance-user");
        UUID plan = jdbc.queryForObject("select id from plans where name = 'Plano Inicial'", UUID.class);
        UUID allocation = jdbc.queryForObject("insert into quota_allocations (account_id, plan_id, period_start, period_end, total_units, status) values (?, ?, current_date, current_date + 7, 10, 'ACTIVE') returning id", UUID.class, account, plan);
        UUID reservation = jdbc.queryForObject("insert into quota_reservations (account_id, allocation_id, idempotency_key, units, status) values (?, ?, 'maintenance-reservation', 1, 'RESERVED') returning id", UUID.class, account, allocation);
        UUID report = jdbc.queryForObject("insert into reports (account_id, report_type, origin, description, status, reservation_id) values (?, 'EXECUTIVE_SUMMARY', 'API', 'maintenance', 'PENDING', ?) returning id", UUID.class, account, reservation);
        jdbc.update("insert into report_idempotency (account_id, idempotency_key, report_id, expires_at) values (?, 'expired', ?, current_timestamp - interval '1 minute')", account, report);
        UUID reservation2 = jdbc.queryForObject("insert into quota_reservations (account_id, allocation_id, idempotency_key, units, status) values (?, ?, 'maintenance-reservation-2', 1, 'RESERVED') returning id", UUID.class, account, allocation);
        UUID report2 = jdbc.queryForObject("insert into reports (account_id, report_type, origin, description, status, reservation_id) values (?, 'EXECUTIVE_SUMMARY', 'API', 'maintenance-active', 'PENDING', ?) returning id", UUID.class, account, reservation2);
        jdbc.update("insert into report_idempotency (account_id, idempotency_key, report_id, expires_at) values (?, 'active', ?, current_timestamp + interval '1 hour')", account, report2);
        jdbc.update("insert into telegram_conversations (telegram_user_id, account_id, chat_id, state, updated_at) values (7001, ?, 7001, 'TYPE_SELECTION', current_timestamp - interval '31 minutes')", account);
        jdbc.update("insert into telegram_conversations (telegram_user_id, account_id, chat_id, state, updated_at) values (7002, ?, 7002, 'TYPE_SELECTION', current_timestamp)", account);

        maintenance.cleanupDatabase();

        assertThat(jdbc.queryForObject("select count(*) from report_idempotency where idempotency_key = 'expired'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from report_idempotency where idempotency_key = 'active'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from telegram_conversations where telegram_user_id = 7001", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from telegram_conversations where telegram_user_id = 7002", Integer.class)).isEqualTo(1);
    }

    @Test
    void minioCleanupUsesCutoffAndPreservesRecentTemporaries() throws Exception {
        String oldCandidate = storage.putTemporary("old".getBytes(), "text/plain");
        storage.cleanupExpiredTemporaryObjects(Instant.now().plusSeconds(5));
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> storage.get(oldCandidate));

        String recent = storage.putTemporary("recent".getBytes(), "text/plain");
        storage.cleanupExpiredTemporaryObjects(Instant.now().minusSeconds(5));
        try (var object = storage.get(recent)) {
            assertThat(object.readAllBytes()).containsExactly("recent".getBytes());
        }
    }

    @TestConfiguration
    static class TestSecurityConfiguration {
        @Bean @Primary JwtDecoder jwtDecoder() { return token -> { throw new BadJwtException("invalid token"); }; }
    }

    private UUID accountId(String subject) {
        try {
            String response = mockMvc.perform(get("/api/v1/me").with(jwt().jwt(token -> token.subject(subject))))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            return UUID.fromString(JsonPath.read(response, "$.id"));
        } catch (Exception exception) { throw new IllegalStateException(exception); }
    }
}
