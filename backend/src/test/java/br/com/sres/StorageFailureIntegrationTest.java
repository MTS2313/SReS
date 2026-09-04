package br.com.sres;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayOutputStream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "sres.storage.endpoint=http://127.0.0.1:1",
        "sres.storage.access-key=minio_dev",
        "sres.storage.secret-key=change-me-development-only",
        "sres.storage.bucket=sres-reports-failure"
})
@Import(StorageFailureIntegrationTest.TestSecurityConfiguration.class)
class StorageFailureIntegrationTest {
    static { System.setProperty("api.version", "1.44"); }

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.6")
            .withDatabaseName("sres").withUsername("sres_dev").withPassword("change-me-development-only");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("delete from report_idempotency");
        jdbc.update("delete from report_files");
        jdbc.update("delete from reports");
        jdbc.update("delete from quota_ledger");
        jdbc.update("delete from quota_reservations");
        jdbc.update("delete from quota_allocations");
        jdbc.update("delete from accounts");
    }

    @Test
    void unavailableMinioDoesNotPersistReportOrReservation() throws Exception {
        mockMvc.perform(multipart("/api/v1/reports")
                        .file(new org.springframework.mock.web.MockMultipartFile("file", "input.pdf", "application/pdf", pdf()))
                        .param("type", "EXECUTIVE_SUMMARY").param("description", "falha de storage")
                        .header("Idempotency-Key", "storage-failure-key")
                        .with(jwt().jwt(jwt -> jwt.subject("storage-failure-user"))))
                .andExpect(status().isBadGateway());
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("select count(*) from reports", Integer.class)).isZero();
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("select count(*) from quota_reservations", Integer.class)).isZero();
    }

    private static byte[] pdf() throws Exception {
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            return output.toByteArray();
        }
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestSecurityConfiguration {
        @Bean JwtDecoder jwtDecoder() { return token -> { throw new BadJwtException("invalid token"); }; }
    }
}
