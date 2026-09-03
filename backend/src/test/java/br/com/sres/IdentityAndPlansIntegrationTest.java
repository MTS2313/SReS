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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(IdentityAndPlansIntegrationTest.TestSecurityConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdentityAndPlansIntegrationTest {
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
    }

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired KeycloakRoleConverter roleConverter;

    @BeforeEach
    void cleanBusinessData() {
        jdbcTemplate.update("delete from account_audit");
        jdbcTemplate.update("delete from accounts");
        jdbcTemplate.update("delete from plans where name <> 'Plano Inicial'");
        jdbcTemplate.update("update plans set active = true, is_default = true, weekly_limit = 10 where name = 'Plano Inicial'");
    }

    @Test
    void rejectsRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userTokenCanAccessMeAndIsProvisionedWithInitialPlan() throws Exception {
        mockMvc.perform(get("/api/v1/me").with(userJwt("subject-user", "ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("subject-user"))
                .andExpect(jsonPath("$.plan.name").value("Plano Inicial"))
                .andExpect(jsonPath("$.plan.weeklyLimit").value(10));
        assertThat(jdbcTemplate.queryForObject("select count(*) from accounts where subject = 'subject-user'", Integer.class)).isEqualTo(1);
    }

    @Test
    void adminTokenCanAccessAdministrativeAccounts() throws Exception {
        mockMvc.perform(get("/api/v1/admin/accounts").with(userJwt("subject-admin", "ROLE_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void userCannotAccessAdministrativeAccounts() throws Exception {
        mockMvc.perform(get("/api/v1/admin/accounts").with(userJwt("subject-user", "ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void concurrentFirstAccessCreatesExactlyOneAccount() throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            List<Callable<Integer>> requests = java.util.stream.IntStream.range(0, 8)
                    .<Callable<Integer>>mapToObj(i -> () -> mockMvc.perform(get("/api/v1/me").with(userJwt("same-subject", "ROLE_USER")))
                            .andReturn().getResponse().getStatus())
                    .toList();
            for (var future : executor.invokeAll(requests)) {
                assertThat(future.get()).isEqualTo(200);
            }
        }
        assertThat(jdbcTemplate.queryForObject("select count(*) from accounts where subject = 'same-subject'", Integer.class)).isEqualTo(1);
    }

    @Test
    void adminCanBlockAndUnblockAnAccountAndBlockedUserCanStillReadMe() throws Exception {
        String accountId = accountIdFor("blocked-subject");
        mockMvc.perform(post("/api/v1/admin/accounts/" + accountId + "/block")
                        .with(userJwt("admin-subject", "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
        mockMvc.perform(get("/api/v1/me").with(userJwt("blocked-subject", "ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
        mockMvc.perform(post("/api/v1/admin/accounts/" + accountId + "/unblock")
                        .with(userJwt("admin-subject", "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void adminCanCreateAndAlterPlans() throws Exception {
        String body = "{\"name\":\"Pro\",\"weeklyLimit\":25,\"active\":true}";
        String planId = JsonPath.read(mockMvc.perform(post("/api/v1/admin/plans").with(userJwt("admin-subject", "ROLE_ADMIN"))
                        .contentType(APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn()
                .getResponse().getContentAsString(), "$.id");
        mockMvc.perform(patch("/api/v1/admin/plans/" + planId).with(userJwt("admin-subject", "ROLE_ADMIN"))
                        .contentType(APPLICATION_JSON).content("{\"name\":\"Pro Plus\",\"weeklyLimit\":30}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pro Plus"))
                .andExpect(jsonPath("$.weeklyLimit").value(30));
    }

    @Test
    void adminCanSetOnlyOneDefaultPlan() throws Exception {
        String planId = JsonPath.read(mockMvc.perform(post("/api/v1/admin/plans").with(userJwt("admin-subject", "ROLE_ADMIN"))
                        .contentType(APPLICATION_JSON).content("{\"name\":\"Default Candidate\",\"weeklyLimit\":20,\"active\":true}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        mockMvc.perform(post("/api/v1/admin/plans/" + planId + "/default").with(userJwt("admin-subject", "ROLE_ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isDefault").value(true));
        assertThat(jdbcTemplate.queryForObject("select count(*) from plans where is_default = true", Integer.class)).isEqualTo(1);
    }

    @Test
    void inactivePlanCannotBeAssignedToANewAccount() throws Exception {
        String accountId = accountIdFor("assignment-subject");
        String planId = JsonPath.read(mockMvc.perform(post("/api/v1/admin/plans").with(userJwt("admin-subject", "ROLE_ADMIN"))
                        .contentType(APPLICATION_JSON).content("{\"name\":\"Inactive\",\"weeklyLimit\":5,\"active\":false}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        mockMvc.perform(put("/api/v1/admin/accounts/" + accountId + "/plan").with(userJwt("admin-subject", "ROLE_ADMIN"))
                        .contentType(APPLICATION_JSON).content("{\"planId\":\"" + planId + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void databaseRejectsASecondDefaultPlan() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbcTemplate.update(
                        "insert into plans (name, weekly_limit, active, is_default) values (?, ?, ?, ?)",
                        "Duplicate Default", 5, true, true))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void keycloakRealmRolesBecomeSpringRoles() {
        var token = Jwt.withTokenValue("test-token").header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("USER", "ADMIN"))).build();
        assertThat(roleConverter.convert(token)).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @TestConfiguration
    static class TestSecurityConfiguration {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> { throw new BadJwtException("invalid token"); };
        }
    }

    private String accountIdFor(String subject) throws Exception {
        return JsonPath.read(mockMvc.perform(get("/api/v1/me").with(userJwt(subject, "ROLE_USER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor userJwt(String subject, String role) {
        return jwt().jwt(jwt -> jwt.subject(subject).claim("preferred_username", subject).claim("email", subject + "@example.test"))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(role));
    }
}
