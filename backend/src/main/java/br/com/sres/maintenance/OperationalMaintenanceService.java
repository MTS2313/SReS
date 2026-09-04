package br.com.sres.maintenance;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationalMaintenanceService {
    private final JdbcTemplate jdbc;

    public OperationalMaintenanceService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Scheduled(fixedDelayString = "${sres.maintenance.database-interval-ms:3600000}")
    @Transactional
    public void cleanupDatabase() {
        jdbc.update("delete from report_idempotency where expires_at < current_timestamp");
        jdbc.update("delete from telegram_conversations where updated_at < current_timestamp - interval '30 minutes'");
    }
}
