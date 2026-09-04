package br.com.sres;

import br.com.sres.ollama.GenerationResult;
import br.com.sres.ollama.ReportGenerator;
import br.com.sres.processing.ReportProcessingWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RecordApplicationEvents
class ReportProcessingIntegrationTest {
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
        registry.add("sres.storage.access-key", () -> "minio_dev");
        registry.add("sres.storage.secret-key", () -> "change-me-development-only");
        registry.add("sres.storage.bucket", () -> "sres-processing-test");
        registry.add("sres.processing.enabled", () -> "true");
        registry.add("sres.processing.initial-delay-ms", () -> "3600000");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired ReportProcessingWorker worker;
    @Autowired ReportGenerator generator;
    @Autowired ApplicationEvents applicationEvents;

    @BeforeEach
    void clean() {
        jdbc.update("delete from report_attempts");
        jdbc.update("delete from report_files where file_kind = 'OUTPUT_MARKDOWN'");
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
    void processesPendingReportWithVersionedPromptAndPersistsMarkdownMetricsAndConfirmedQuota() {
        UUID reportId = report("EXECUTIVE_SUMMARY", "entrada autorizada", "processing-user");
        worker.processOne();
        assertThat(status(reportId)).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("select object_key from report_files where report_id = ? and file_kind = 'OUTPUT_MARKDOWN'", String.class, reportId))
                .startsWith("accounts/").contains("/reports/" + reportId + "/output.md");
        assertThat(jdbc.queryForObject("select count(*) from report_attempts where report_id = ? and status = 'SUCCEEDED'", Integer.class, reportId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select model from usage_metrics where reservation_id = (select reservation_id from reports where id = ?)", String.class, reportId)).isEqualTo("test-model");
        assertThat(jdbc.queryForObject("select status from quota_reservations where id = (select reservation_id from reports where id = ?)", String.class, reportId)).isEqualTo("CONFIRMED");
        assertThat(((RecordingGenerator) generator).lastPrompt()).contains("EXECUTIVE_SUMMARY").contains("entrada autorizada");
        assertThat(applicationEvents.stream(ReportProcessingWorker.ReportCompletedEvent.class)).hasSize(1);
    }

    @Test
    void usesExtractedPdfTextWithoutLeakingOtherAccountData() {
        UUID reportId = report("DETAILED_ANALYSIS", "pdf text only", "pdf-processing-user");
        jdbc.update("update reports set extracted_text = ? where id = ?", "texto extraído autorizado", reportId);
        worker.processOne();
        assertThat(((RecordingGenerator) generator).lastPrompt()).contains("texto extraído autorizado").doesNotContain("other-account");
    }

    @Test
    void retriesTechnicalFailureOnceThenFailsAndReleasesOriginalReservation() {
        UUID reportId = report("STRUCTURED_EXTRACTION", "retry input", "retry-user");
        ((RecordingGenerator) generator).failNext(2);
        worker.processOne();
        worker.processOne();
        assertThat(status(reportId)).isEqualTo("FAILED");
        assertThat(jdbc.queryForObject("select count(*) from report_attempts where report_id = ?", Integer.class, reportId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select status from quota_reservations where id = (select reservation_id from reports where id = ?)", String.class, reportId)).isEqualTo("RELEASED");
    }

    @Test
    void recoversStaleProcessingAndDoesNotProcessSameReportTwice() {
        UUID reportId = report("EXECUTIVE_SUMMARY", "stale input", "stale-user");
        jdbc.update("insert into report_attempts (report_id, attempt_number, status, started_at, finished_at) values (?, 1, 'FAILED', current_timestamp - interval '31 minutes', current_timestamp - interval '30 minutes')", reportId);
        jdbc.update("update reports set status = 'PROCESSING', updated_at = current_timestamp - interval '31 minutes' where id = ?", reportId);
        worker.processOne();
        assertThat(status(reportId)).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("select count(*) from report_attempts where report_id = ?", Integer.class, reportId)).isEqualTo(2);
    }

    @Test
    void concurrentWorkersClaimOnlyOneReport() throws Exception {
        UUID reportId = report("EXECUTIVE_SUMMARY", "concurrent input", "worker-user");
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> { start.await(); worker.processOne(); return null; });
            var two = executor.submit(() -> { start.await(); worker.processOne(); return null; });
            start.countDown();
            one.get(30, TimeUnit.SECONDS);
            two.get(30, TimeUnit.SECONDS);
        }
        assertThat(status(reportId)).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("select count(*) from report_attempts where report_id = ?", Integer.class, reportId)).isEqualTo(1);
    }

    @Test
    void invalidStateIsNotProcessed() {
        UUID reportId = report("EXECUTIVE_SUMMARY", "invalid state", "invalid-state-user");
        jdbc.update("update reports set status = 'COMPLETED' where id = ?", reportId);
        worker.processOne();
        assertThat(jdbc.queryForObject("select count(*) from report_attempts where report_id = ?", Integer.class, reportId)).isZero();
    }

    private UUID report(String type, String description, String subject) {
        UUID planId = jdbc.queryForObject("select id from plans where name = 'Plano Inicial'", UUID.class);
        UUID accountId = jdbc.queryForObject("insert into accounts (subject, email, username, status, plan_id) values (?, ?, ?, 'ACTIVE', ?) returning id", UUID.class, subject, subject + "@test", subject, planId);
        UUID allocationId = jdbc.queryForObject("insert into quota_allocations (account_id, plan_id, period_start, period_end, total_units, reserved_units, status) values (?, ?, current_date, current_date + 7, 10, 1, 'ACTIVE') returning id", UUID.class, accountId, planId);
        UUID reservationId = jdbc.queryForObject("insert into quota_reservations (account_id, allocation_id, idempotency_key, units, status) values (?, ?, ?, 1, 'RESERVED') returning id", UUID.class, accountId, allocationId, UUID.randomUUID().toString());
        return jdbc.queryForObject("insert into reports (account_id, report_type, origin, description, status, reservation_id) values (?, ?, 'API', ?, 'PENDING', ?) returning id", UUID.class, accountId, type, description, reservationId);
    }

    private String status(UUID id) { return jdbc.queryForObject("select status from reports where id = ?", String.class, id); }

    @TestConfiguration
    static class TestSecurityConfiguration {
        @Bean JwtDecoder jwtDecoder() { return token -> { throw new BadJwtException("invalid token"); }; }
        @Bean ReportGenerator reportGenerator() { return new RecordingGenerator(); }
    }

    static class RecordingGenerator implements ReportGenerator {
        private volatile String prompt;
        private int failures;
        @Override public GenerationResult generate(String reportType, String prompt) {
            this.prompt = prompt;
            if (failures > 0) { failures--; throw new RuntimeException("temporary model failure"); }
            return new GenerationResult("# Resultado\n\nProcessado", "test-model", 12, 8, 20);
        }
        void failNext(int count) { failures = count; }
        String lastPrompt() { return prompt; }
    }
}
