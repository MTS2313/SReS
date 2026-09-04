package br.com.sres;

import br.com.sres.telegram.TelegramConversationService;
import br.com.sres.telegram.TelegramDeliveryService;
import br.com.sres.telegram.TelegramGateway;
import br.com.sres.telegram.TelegramLinkResponse;
import br.com.sres.telegram.TelegramLinkService;
import br.com.sres.telegram.TelegramUpdate;
import br.com.sres.processing.ReportProcessingWorker;
import com.jayway.jsonpath.JsonPath;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TelegramIntegrationTest {
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
        registry.add("sres.storage.bucket", () -> "sres-telegram-test");
        registry.add("sres.processing.enabled", () -> "false");
        registry.add("sres.integrations.telegram.enabled", () -> "false");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.test.web.servlet.MockMvc mockMvc;
    @Autowired TelegramLinkService links;
    @Autowired TelegramConversationService conversations;
    @Autowired TelegramDeliveryService deliveries;
    @Autowired TelegramGateway gateway;

    @BeforeEach
    void clean() {
        jdbc.update("delete from telegram_delivery_attempts");
        jdbc.update("delete from telegram_deliveries");
        jdbc.update("delete from telegram_conversations");
        jdbc.update("delete from telegram_processed_updates");
        jdbc.update("delete from telegram_link_codes");
        jdbc.update("delete from telegram_links");
        jdbc.update("delete from report_attempts");
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
        ((FakeTelegramGateway) gateway).reset();
    }

    @Test
    void generatesUnpredictableTenMinuteSingleUseLinkCode() throws Exception {
        String account = accountId("link-user");
        var first = links.generate(UUID.fromString(account));
        assertThat(first.code()).hasSize(32);
        assertThat(first.expiresAt()).isAfter(Instant.now().plusSeconds(9 * 60));
        assertThat(links.consume(first.code(), 1001L, 2001L)).isTrue();
        assertThat(links.consume(first.code(), 1001L, 2001L)).isFalse();
    }

    @Test
    void rejectsExpiredCodeAndDuplicateCardinality() throws Exception {
        UUID first = UUID.fromString(accountId("first-link-user"));
        UUID second = UUID.fromString(accountId("second-link-user"));
        TelegramLinkResponse code = links.generate(first);
        jdbc.update("update telegram_link_codes set expires_at = current_timestamp - interval '1 minute' where code_hash = ?", links.hashForTest(code.code()));
        assertThat(links.consume(code.code(), 1002L, 2002L)).isFalse();
        TelegramLinkResponse valid = links.generate(first);
        assertThat(links.consume(valid.code(), 1002L, 2002L)).isTrue();
        TelegramLinkResponse other = links.generate(second);
        assertThat(links.consume(other.code(), 1002L, 2002L)).isFalse();
    }

    @Test
    void protectedEndpointGeneratesLinkAndBlockedAccountCannotGenerateIt() throws Exception {
        mockMvc.perform(post("/api/v1/me/telegram-link").with(jwt().jwt(token -> token.subject("api-link-user"))))
                .andExpect(status().isOk());
        UUID id = UUID.fromString(accountId("blocked-link-user"));
        jdbc.update("update accounts set status = 'BLOCKED' where id = ?", id);
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> links.generate(id));
    }

    @Test
    void duplicateUpdatesHaveNoSecondEffectAndConversationSurvivesRestart() throws Exception {
        UUID accountId = UUID.fromString(accountId("conversation-user"));
        TelegramLinkResponse code = links.generate(accountId);
        assertThat(conversations.process(new TelegramUpdate(1L, 3001L, 4001L, code.code(), null))).isTrue();
        assertThat(conversations.process(new TelegramUpdate(1L, 3001L, 4001L, code.code(), null))).isFalse();
        conversations.process(new TelegramUpdate(2L, 3001L, 4001L, "/start", null));
        conversations.process(new TelegramUpdate(3L, 3001L, 4001L, "DETAILED_ANALYSIS", null));
        assertThat(jdbc.queryForObject("select state from telegram_conversations where telegram_user_id = ?", String.class, 3001L)).isEqualTo("DESCRIPTION");
        assertThat(conversations.load(3001L)).isPresent();
    }

    @Test
    void conversationCollectsTypeDescriptionAndCreatesReportThroughApplicationService() throws Exception {
        UUID accountId = UUID.fromString(accountId("report-telegram-user"));
        TelegramLinkResponse code = links.generate(accountId);
        conversations.process(new TelegramUpdate(10L, 3010L, 4010L, code.code(), null));
        conversations.process(new TelegramUpdate(11L, 3010L, 4010L, "/start", null));
        conversations.process(new TelegramUpdate(12L, 3010L, 4010L, "EXECUTIVE_SUMMARY", null));
        conversations.process(new TelegramUpdate(13L, 3010L, 4010L, "descrição pelo Telegram", null));
        conversations.process(new TelegramUpdate(14L, 3010L, 4010L, "/skip", null));
        conversations.process(new TelegramUpdate(14L, 3010L, 4010L, "/skip", null));
        assertThat(jdbc.queryForObject("select count(*) from reports where account_id = ? and origin = 'TELEGRAM'", Integer.class, accountId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from quota_reservations where account_id = ?", Integer.class, accountId)).isEqualTo(1);
    }

    @Test
    void telegramPdfUsesTheSameValidatedInputRulesAsApi() throws Exception {
        UUID accountId = UUID.fromString(accountId("telegram-pdf-user"));
        TelegramLinkResponse code = links.generate(accountId);
        conversations.process(new TelegramUpdate(15L, 3015L, 4015L, code.code(), null));
        conversations.process(new TelegramUpdate(16L, 3015L, 4015L, "/start", null));
        conversations.process(new TelegramUpdate(17L, 3015L, 4015L, "EXECUTIVE_SUMMARY", null));
        conversations.process(new TelegramUpdate(18L, 3015L, 4015L, "descrição com PDF", null));
        byte[] pdf;
        try (var document = new PDDocument(); var output = new java.io.ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            pdf = output.toByteArray();
        }
        conversations.process(new TelegramUpdate(19L, 3015L, 4015L, null,
                new TelegramUpdate.Document("file-19", "input.pdf", "application/pdf", pdf.length, pdf)));
        assertThat(jdbc.queryForObject("select count(*) from reports where account_id = ? and origin = 'TELEGRAM'", Integer.class, accountId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from report_files where file_kind = 'INPUT_PDF' and report_id in (select id from reports where account_id = ?)", Integer.class, accountId)).isEqualTo(1);
    }

    @Test
    void expiredConversationCanStartAgainAndCancelClearsState() throws Exception {
        UUID accountId = UUID.fromString(accountId("expired-conversation-user"));
        TelegramLinkResponse code = links.generate(accountId);
        conversations.process(new TelegramUpdate(20L, 3020L, 4020L, code.code(), null));
        conversations.process(new TelegramUpdate(21L, 3020L, 4020L, "/start", null));
        jdbc.update("update telegram_conversations set updated_at = current_timestamp - interval '31 minutes' where telegram_user_id = 3020");
        conversations.process(new TelegramUpdate(22L, 3020L, 4020L, "EXECUTIVE_SUMMARY", null));
        conversations.process(new TelegramUpdate(23L, 3020L, 4020L, "EXECUTIVE_SUMMARY", null));
        assertThat(jdbc.queryForObject("select state from telegram_conversations where telegram_user_id = ?", String.class, 3020L)).isEqualTo("DESCRIPTION");
        conversations.process(new TelegramUpdate(24L, 3020L, 4020L, "/cancel", null));
        assertThat(conversations.load(3020L)).isEmpty();
    }

    @Test
    void concurrentSameUpdateIsDeduplicatedInPostgres() throws Exception {
        UUID accountId = UUID.fromString(accountId("concurrent-update-user"));
        TelegramLinkResponse code = links.generate(accountId);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> conversations.process(new TelegramUpdate(30L, 3030L, 4030L, code.code(), null)));
            var two = executor.submit(() -> conversations.process(new TelegramUpdate(30L, 3030L, 4030L, code.code(), null)));
            assertThat(java.util.List.of(one.get(), two.get())).containsExactlyInAnyOrder(true, false);
        }
        assertThat(jdbc.queryForObject("select count(*) from telegram_links where telegram_user_id = ?", Integer.class, 3030L)).isEqualTo(1);
    }

    @Test
    void completedDeliveryRetriesThreeTimesWithoutChangingReportOrQuota() throws Exception {
        UUID reportId = reportWithReservation("delivery-user");
        ((FakeTelegramGateway) gateway).failNext(3);
        deliveries.onCompleted(new ReportProcessingWorker.ReportCompletedEvent(reportId, accountFor("delivery-user")));
        forceDeliveryDue(reportId);
        deliveries.deliverDue();
        forceDeliveryDue(reportId);
        deliveries.deliverDue();
        assertThat(jdbc.queryForObject("select status from telegram_deliveries where report_id = ?", String.class, reportId)).isEqualTo("FAILED");
        assertThat(jdbc.queryForObject("select status from reports where id = ?", String.class, reportId)).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("select status from quota_reservations where id = (select reservation_id from reports where id = ?)", String.class, reportId)).isEqualTo("CONFIRMED");
    }

    @TestConfiguration
    static class TestSecurityConfiguration {
        @Bean JwtDecoder jwtDecoder() { return token -> { throw new BadJwtException("invalid token"); }; }
        @Bean @Primary TelegramGateway telegramGateway() { return new FakeTelegramGateway(); }
    }

    static class FakeTelegramGateway implements TelegramGateway {
        private int failures;
        @Override public void sendMessage(long chatId, String message) { }
        @Override public void sendResult(long chatId, String summary, byte[] markdown) { if (failures-- > 0) throw new RuntimeException("telegram unavailable"); }
        @Override public byte[] download(TelegramUpdate.Document document) { return document.content(); }
        void failNext(int amount) { failures = amount; }
        void reset() { failures = 0; }
    }

    private String accountId(String subject) throws Exception {
        return JsonPath.read(mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/me").with(jwt().jwt(token -> token.subject(subject))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    private UUID accountFor(String subject) throws Exception { return UUID.fromString(accountId(subject)); }

    private UUID reportWithReservation(String subject) throws Exception {
        UUID account = UUID.fromString(accountId(subject));
        UUID plan = jdbc.queryForObject("select id from plans where name = 'Plano Inicial'", UUID.class);
        UUID allocation = jdbc.queryForObject("insert into quota_allocations (account_id, plan_id, period_start, period_end, total_units, reserved_units, consumed_units, status) values (?, ?, current_date, current_date + 7, 10, 0, 1, 'ACTIVE') returning id", UUID.class, account, plan);
        UUID reservation = jdbc.queryForObject("insert into quota_reservations (account_id, allocation_id, idempotency_key, units, status) values (?, ?, ?, 1, 'CONFIRMED') returning id", UUID.class, account, allocation, UUID.randomUUID().toString());
        UUID report = jdbc.queryForObject("insert into reports (account_id, report_type, origin, description, status, reservation_id) values (?, 'EXECUTIVE_SUMMARY', 'TELEGRAM', 'delivery', 'COMPLETED', ?) returning id", UUID.class, account, reservation);
        jdbc.update("insert into telegram_links (account_id, telegram_user_id, chat_id) values (?, 9001, 9001)", account);
        return report;
    }

    private void forceDeliveryDue(UUID reportId) { jdbc.update("update telegram_deliveries set next_attempt_at = current_timestamp - interval '1 second' where report_id = ?", reportId); }
}
