package br.com.sres.administration;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/audit")
public class AdminAuditController {
    private final JdbcTemplate jdbc;

    public AdminAuditController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Map<String, Object>> list() {
        return jdbc.queryForList("select id, account_id, actor_subject, action, reason, created_at from account_audit order by created_at desc, id desc");
    }
}
