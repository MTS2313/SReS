package br.com.sres;

import com.jayway.jsonpath.JsonPath;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ReportsAndStorageIntegrationTest.TestSecurityConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReportsAndStorageIntegrationTest {
    static { System.setProperty("api.version", "1.44"); }

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.6")
            .withDatabaseName("sres")
            .withUsername("sres_dev")
            .withPassword("change-me-development-only");

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
        registry.add("sres.storage.access-key", () -> "minio_dev");
        registry.add("sres.storage.secret-key", () -> "change-me-development-only");
        registry.add("sres.storage.bucket", () -> "sres-reports-test");
    }

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("delete from report_files");
        jdbc.update("delete from report_idempotency");
        jdbc.update("delete from reports");
        jdbc.update("delete from usage_metrics");
        jdbc.update("delete from quota_ledger");
        jdbc.update("delete from quota_reservations");
        jdbc.update("delete from quota_allocations");
        jdbc.update("delete from account_audit");
        jdbc.update("delete from accounts");
        jdbc.update("delete from plans where name <> 'Plano Inicial'");
        jdbc.update("update plans set active = true, is_default = true, weekly_limit = 10 where name = 'Plano Inicial'");
    }

    @Test
    void acceptsTextOnlyAndReturnsPendingWithLocation() throws Exception {
        var result = create("report-user", "EXECUTIVE_SUMMARY", "texto de entrada", null, "report-key-1")
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/v1/reports/[0-9a-f-]+")))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        assertThat((String) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).isNotNull();
        assertThat(jdbc.queryForObject("select count(*) from reports", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from quota_reservations", Integer.class)).isEqualTo(1);
    }

    @Test
    void storesValidPdfPrivatelyAndAllowsOwnedDownload() throws Exception {
        String id = reportId(create("pdf-user", "DETAILED_ANALYSIS", "com pdf", pdfWithText("texto extraído"), "pdf-key"));
        mockMvc.perform(get("/api/v1/reports/" + id).with(userJwt("pdf-user")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING"));
        mockMvc.perform(get("/api/v1/reports/" + id + "/input").with(userJwt("pdf-user")))
                .andExpect(status().isOk()).andExpect(header().string("Content-Type", "application/pdf"));
        assertThat(jdbc.queryForObject("select object_key from report_files where report_id = ?", String.class, UUID.fromString(id)))
                .startsWith("accounts/").contains("/reports/" + id + "/");
        assertThat(jdbc.queryForObject("select extracted_text from reports where id = ?", String.class, UUID.fromString(id)))
                .contains("texto extraído");
    }

    @Test
    void rejectsInvalidPdfBeforeReservingQuota() throws Exception {
        mockMvc.perform(multipart("/api/v1/reports")
                        .file(new org.springframework.mock.web.MockMultipartFile("file", "bad.pdf", "application/pdf", "not pdf".getBytes(StandardCharsets.UTF_8)))
                        .param("type", "EXECUTIVE_SUMMARY")
                        .param("description", "entrada inválida")
                        .header("Idempotency-Key", "invalid-key")
                        .with(userJwt("invalid-pdf-user")))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from quota_reservations", Integer.class)).isZero();
    }

    @Test
    void rejectsOversizedPdfBeforeReservingQuota() throws Exception {
        byte[] oversized = new byte[(int) (10L * 1024 * 1024 + 1)];
        oversized[0] = '%'; oversized[1] = 'P'; oversized[2] = 'D'; oversized[3] = 'F'; oversized[4] = '-';
        mockMvc.perform(multipart("/api/v1/reports")
                        .file(new org.springframework.mock.web.MockMultipartFile("file", "large.pdf", "application/pdf", oversized))
                        .param("type", "EXECUTIVE_SUMMARY").param("description", "grande")
                        .header("Idempotency-Key", "large-key").with(userJwt("large-pdf-user")))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from quota_reservations", Integer.class)).isZero();
    }

    @Test
    void rejectsPdfWithMoreThanFiftyPagesBeforeReservingQuota() throws Exception {
        create("many-pages-user", "EXECUTIVE_SUMMARY", "muitas páginas", pdf(51), "many-pages-key")
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from quota_reservations", Integer.class)).isZero();
    }

    @Test
    void repeatedIdempotencyKeyDoesNotDuplicateReportOrQuota() throws Exception {
        var first = create("idempotent-user", "STRUCTURED_EXTRACTION", "entrada", null, "same-key").andExpect(status().isAccepted()).andReturn();
        var second = create("idempotent-user", "STRUCTURED_EXTRACTION", "entrada", null, "same-key").andExpect(status().isAccepted()).andReturn();
        assertThat(reportId(first)).isEqualTo(reportId(second));
        assertThat(jdbc.queryForObject("select count(*) from reports", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from quota_reservations", Integer.class)).isEqualTo(1);
    }

    @Test
    void concurrentSameKeyCreatesOneReportAndOneReservation() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Callable<Integer>> calls = List.of(
                    () -> create("concurrent-report-user", "EXECUTIVE_SUMMARY", "entrada", null, "concurrent-key").andReturn().getResponse().getStatus(),
                    () -> create("concurrent-report-user", "EXECUTIVE_SUMMARY", "entrada", null, "concurrent-key").andReturn().getResponse().getStatus());
            assertThat(executor.invokeAll(calls).stream().map(future -> {
                try { return future.get(); } catch (Exception exception) { throw new RuntimeException(exception); }
            }).toList()).containsExactly(202, 202);
        }
        assertThat(jdbc.queryForObject("select count(*) from reports", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from quota_reservations", Integer.class)).isEqualTo(1);
    }

    @Test
    void ownershipAndPaginationAreEnforced() throws Exception {
        String id = reportId(create("owner-user", "EXECUTIVE_SUMMARY", "entrada", null, "owner-key"));
        mockMvc.perform(get("/api/v1/reports/" + id).with(userJwt("other-user")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/reports").param("page", "0").param("size", "10").param("sort", "createdAt,desc")
                        .with(userJwt("owner-user")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(id));
    }

    @Test
    void blockedAccountCannotCreateReport() throws Exception {
        String accountId = accountId("blocked-report-user");
        mockMvc.perform(post("/api/v1/admin/accounts/" + accountId + "/block").with(userJwt("admin-subject", "ROLE_ADMIN")))
                .andExpect(status().isOk());
        create("blocked-report-user", "EXECUTIVE_SUMMARY", "entrada", null, "blocked-key")
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from reports", Integer.class)).isZero();
    }

    @Test
    void quotaExhaustionRejectsReportWithoutCreatingIt() throws Exception {
        String accountId = accountId("exhausted-report-user");
        mockMvc.perform(post("/api/v1/admin/accounts/" + accountId + "/quota/adjust").with(userJwt("admin-subject", "ROLE_ADMIN"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"units\":-10,\"reason\":\"exhaust test\"}"))
                .andExpect(status().isOk());
        create("exhausted-report-user", "EXECUTIVE_SUMMARY", "entrada", null, "exhausted-key")
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from reports", Integer.class)).isZero();
    }

    private org.springframework.test.web.servlet.ResultActions create(String subject, String type, String description,
                                                                        byte[] pdf, String key) throws Exception {
        var builder = multipart("/api/v1/reports")
                .param("type", type).param("description", description)
                .header("Idempotency-Key", key).with(userJwt(subject));
        if (pdf == null) return mockMvc.perform(builder);
        return mockMvc.perform(multipart("/api/v1/reports")
                .file(new org.springframework.mock.web.MockMultipartFile("file", "input.pdf", "application/pdf", pdf))
                .param("type", type).param("description", description)
                .header("Idempotency-Key", key).with(userJwt(subject)));
    }

    private String reportId(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String reportId(ResultActions actions) throws Exception {
        return reportId(actions.andReturn());
    }

    private String accountId(String subject) throws Exception {
        return JsonPath.read(mockMvc.perform(get("/api/v1/me").with(userJwt(subject)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    private static byte[] pdf(int pages) throws Exception {
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) document.addPage(new PDPage());
            document.save(output);
            return output.toByteArray();
        }
    }

    private static byte[] pdfWithText(String text) throws Exception {
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            var page = new PDPage();
            document.addPage(page);
            try (var content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.showText(text);
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private static JwtRequestPostProcessor userJwt(String subject) { return userJwt(subject, "ROLE_USER"); }

    private static JwtRequestPostProcessor userJwt(String subject, String role) {
        return jwt().jwt(jwt -> jwt.subject(subject).claim("preferred_username", subject).claim("email", subject + "@example.test"))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(role));
    }

    @TestConfiguration
    static class TestSecurityConfiguration {
        @Bean JwtDecoder jwtDecoder() { return token -> { throw new BadJwtException("invalid token"); }; }
    }
}
