package br.com.sres;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(QuotaAndCostsIntegrationTest.TestSecurityConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QuotaAndCostsIntegrationTest {
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
        if (!postgres.isRunning()) postgres.start();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("sres.cost.value-per-million-tokens", () -> "0.01");
    }

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanBusinessData() {
        jdbcTemplate.update("delete from usage_metrics");
        jdbcTemplate.update("delete from quota_ledger");
        jdbcTemplate.update("delete from quota_reservations");
        jdbcTemplate.update("delete from quota_allocations");
        jdbcTemplate.update("delete from account_audit");
        jdbcTemplate.update("delete from accounts");
        jdbcTemplate.update("delete from plans where name <> 'Plano Inicial'");
        jdbcTemplate.update("update plans set active = true, is_default = true, weekly_limit = 10 where name = 'Plano Inicial'");
    }

    @Test
    void reportsWeeklyAllocationInSaoPauloAndRenewsWithoutDuplicates() throws Exception {
        String accountId = accountIdFor("quota-user");
        mockMvc.perform(get("/api/v1/usage").with(userJwt("quota-user", "ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.available").value(10))
                .andExpect(jsonPath("$.reserved").value(0))
                .andExpect(jsonPath("$.consumed").value(0))
                .andExpect(jsonPath("$.nextRenewal").isNotEmpty());
        String nextRenewal = mockMvc.perform(get("/api/v1/usage").with(userJwt("quota-user", "ROLE_USER")))
                .andReturn().getResponse().getContentAsString();
        assertThat(LocalDate.parse(JsonPath.read(nextRenewal, "$.nextRenewal").toString()).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        jdbcTemplate.update("update quota_allocations set period_start = period_start - 7, period_end = period_end - 7 where account_id = ? and status = 'ACTIVE'", java.util.UUID.fromString(accountId));
        mockMvc.perform(get("/api/v1/usage").with(userJwt("quota-user", "ROLE_USER"))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(10));
        assertThat(jdbcTemplate.queryForObject("select count(*) from quota_allocations where account_id = ? and status = 'ACTIVE'", Integer.class, java.util.UUID.fromString(accountId))).isEqualTo(1);
        mockMvc.perform(post("/api/v1/admin/usage/renew").with(userJwt("admin-subject", "ROLE_ADMIN")))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject("select count(*) from quota_allocations where account_id = ? and status = 'ACTIVE'", Integer.class, java.util.UUID.fromString(accountId))).isEqualTo(1);
    }

    @Test
    void reservationConfirmationReleaseAreAtomicAndIdempotent() throws Exception {
        String first = reserve("reservation-user", "key-1");
        String replay = reserve("reservation-user", "key-1");
        assertThat(replay).isEqualTo(first);
        mockMvc.perform(post("/api/v1/usage/reservations/" + first + "/confirm").with(userJwt("reservation-user", "ROLE_USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        mockMvc.perform(post("/api/v1/usage/reservations/" + first + "/confirm").with(userJwt("reservation-user", "ROLE_USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        mockMvc.perform(get("/api/v1/usage").with(userJwt("reservation-user", "ROLE_USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.consumed").value(1));
        String released = reserve("reservation-user", "key-2");
        mockMvc.perform(post("/api/v1/usage/reservations/" + released + "/release").with(userJwt("reservation-user", "ROLE_USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RELEASED"));
    }

    @Test
    void twoConcurrentReservationsCannotConsumeOneAvailableUnitTwice() throws Exception {
        String accountId = accountIdFor("race-user");
        mockMvc.perform(post("/api/v1/admin/accounts/" + accountId + "/quota/adjust")
                        .with(userJwt("admin-subject", "ROLE_ADMIN")).contentType(APPLICATION_JSON)
                        .content("{\"units\":-9,\"reason\":\"concurrency test\"}"))
                .andExpect(status().isOk());
        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Callable<Integer>> calls = List.of(
                    () -> mockMvc.perform(post("/api/v1/usage/reservations").with(userJwt("race-user", "ROLE_USER"))
                            .header("Idempotency-Key", "race-a")).andReturn().getResponse().getStatus(),
                    () -> mockMvc.perform(post("/api/v1/usage/reservations").with(userJwt("race-user", "ROLE_USER"))
                            .header("Idempotency-Key", "race-b")).andReturn().getResponse().getStatus());
            var statuses = executor.invokeAll(calls).stream().map(f -> {
                try { return f.get(); } catch (Exception e) { throw new RuntimeException(e); }
            }).toList();
            assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(jdbcTemplate.queryForObject("select reserved_units from quota_allocations where account_id = ? and status = 'ACTIVE'", Integer.class, java.util.UUID.fromString(accountId))).isEqualTo(1);
    }

    @Test
    void blockedAccountCannotStartConsumption() throws Exception {
        String accountId = accountIdFor("blocked-quota-user");
        mockMvc.perform(post("/api/v1/admin/accounts/" + accountId + "/block").with(userJwt("admin-subject", "ROLE_ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/usage/reservations").with(userJwt("blocked-quota-user", "ROLE_USER"))
                        .header("Idempotency-Key", "blocked-key"))
                .andExpect(status().isConflict());
    }

    @Test
    void planChangeResetsCurrentBalanceAndPreservesPreviousHistory() throws Exception {
        String accountId = accountIdFor("plan-reset-user");
        String reservationId = reserve("plan-reset-user", "before-plan-change");
        mockMvc.perform(post("/api/v1/usage/reservations/" + reservationId + "/confirm").with(userJwt("plan-reset-user", "ROLE_USER")))
                .andExpect(status().isOk());
        String planId = JsonPath.read(mockMvc.perform(post("/api/v1/admin/plans").with(userJwt("admin-subject", "ROLE_ADMIN"))
                        .contentType(APPLICATION_JSON).content("{\"name\":\"Reset Plan\",\"weeklyLimit\":25,\"active\":true}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        mockMvc.perform(put("/api/v1/admin/accounts/" + accountId + "/plan").with(userJwt("admin-subject", "ROLE_ADMIN"))
                        .contentType(APPLICATION_JSON).content("{\"planId\":\"" + planId + "\",\"reason\":\"plan reset\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/usage").with(userJwt("plan-reset-user", "ROLE_USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(25)).andExpect(jsonPath("$.available").value(25));
        assertThat(jdbcTemplate.queryForObject("select count(*) from quota_ledger where account_id = ? and entry_type = 'PLAN_RESET'", Integer.class, java.util.UUID.fromString(accountId))).isEqualTo(1);
        Map<String, Object> reset = jdbcTemplate.queryForMap(
                "select actor_subject, reason, value_before, value_after from quota_ledger where account_id = ? and entry_type = 'PLAN_RESET'",
                java.util.UUID.fromString(accountId));
        assertThat(reset).containsEntry("actor_subject", "admin-subject")
                .containsEntry("reason", "plan reset")
                .containsEntry("value_before", 10)
                .containsEntry("value_after", 25);
        assertThat(jdbcTemplate.queryForObject("select count(*) from quota_allocations where account_id = ?", Integer.class, java.util.UUID.fromString(accountId))).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from quota_reservations r join quota_allocations a on a.id = r.allocation_id where a.account_id = ? and r.status = 'CONFIRMED'", Integer.class, java.util.UUID.fromString(accountId))).isEqualTo(1);
        mockMvc.perform(get("/api/v1/admin/accounts/" + accountId + "/usage").with(userJwt("admin-subject", "ROLE_ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(25));
        String secondPlanId = JsonPath.read(mockMvc.perform(post("/api/v1/admin/plans").with(userJwt("admin-subject", "ROLE_ADMIN"))
                        .contentType(APPLICATION_JSON).content("{\"name\":\"Second Reset Plan\",\"weeklyLimit\":30,\"active\":true}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        mockMvc.perform(put("/api/v1/admin/accounts/" + accountId + "/plan").with(userJwt("admin-subject", "ROLE_ADMIN"))
                        .contentType(APPLICATION_JSON).content("{\"planId\":\"" + secondPlanId + "\",\"reason\":\"second plan reset\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(30));
        assertThat(jdbcTemplate.queryForObject("select count(*) from quota_allocations where account_id = ?", Integer.class, java.util.UUID.fromString(accountId))).isEqualTo(3);
    }

    @Test
    void adjustmentRequiresReasonAndIsVisibleInHistory() throws Exception {
        String accountId = accountIdFor("adjustment-user");
        mockMvc.perform(post("/api/v1/admin/accounts/" + accountId + "/quota/adjust")
                        .with(userJwt("admin-subject", "ROLE_ADMIN")).contentType(APPLICATION_JSON)
                        .content("{\"units\":3,\"reason\":\"manual support grant\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(13));
        mockMvc.perform(get("/api/v1/usage/history").with(userJwt("adjustment-user", "ROLE_USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.entryType == 'ADJUSTMENT')]").isNotEmpty());
        Map<String, Object> adjustment = jdbcTemplate.queryForMap(
                "select actor_subject, reason, value_before, value_after from quota_ledger where account_id = ? and entry_type = 'ADJUSTMENT'",
                java.util.UUID.fromString(accountId));
        assertThat(adjustment).containsEntry("actor_subject", "admin-subject")
                .containsEntry("reason", "manual support grant")
                .containsEntry("value_before", 10)
                .containsEntry("value_after", 13);
        mockMvc.perform(post("/api/v1/admin/accounts/" + accountId + "/quota/adjust")
                        .with(userJwt("admin-subject", "ROLE_ADMIN")).contentType(APPLICATION_JSON)
                        .content("{\"units\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userCannotSeeMonetaryCostButAdminCanSeeRecordedMetrics() throws Exception {
        String accountId = accountIdFor("cost-user");
        mockMvc.perform(get("/api/v1/usage").with(userJwt("cost-user", "ROLE_USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estimatedCost").doesNotExist());
        mockMvc.perform(post("/api/v1/admin/accounts/" + accountId + "/usage/metrics")
                        .with(userJwt("admin-subject", "ROLE_ADMIN")).contentType(APPLICATION_JSON)
                        .content("{\"model\":\"dev-model\",\"inputTokens\":1000,\"outputTokens\":500,\"durationMs\":1200,\"attempts\":1}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estimatedCost").value(0.000015));
        mockMvc.perform(get("/api/v1/admin/costs").with(userJwt("admin-subject", "ROLE_ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].estimatedCost").value(0.000015));
        mockMvc.perform(get("/api/v1/admin/costs").with(userJwt("cost-user", "ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration
    static class TestSecurityConfiguration {
        @Bean JwtDecoder jwtDecoder() { return token -> { throw new BadJwtException("invalid token"); }; }
    }

    private String accountIdFor(String subject) throws Exception {
        return JsonPath.read(mockMvc.perform(get("/api/v1/me").with(userJwt(subject, "ROLE_USER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String reserve(String subject, String key) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/v1/usage/reservations").with(userJwt(subject, "ROLE_USER"))
                        .header("Idempotency-Key", key)).andExpect(status().isCreated()).andReturn()
                .getResponse().getContentAsString(), "$.id");
    }

    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor userJwt(String subject, String role) {
        return jwt().jwt(jwt -> jwt.subject(subject).claim("preferred_username", subject).claim("email", subject + "@example.test"))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(role));
    }
}
