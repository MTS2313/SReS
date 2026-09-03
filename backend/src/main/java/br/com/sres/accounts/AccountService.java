package br.com.sres.accounts;

import br.com.sres.plans.PlanResponse;
import br.com.sres.plans.PlanService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {
    private final AccountRepository accounts;
    private final PlanService plans;
    private final AccountMapper mapper;
    private final JdbcTemplate jdbc;

    public AccountService(AccountRepository accounts, PlanService plans, AccountMapper mapper, JdbcTemplate jdbc) {
        this.accounts = accounts; this.plans = plans; this.mapper = mapper; this.jdbc = jdbc;
    }

    @Transactional
    public AccountResponse me(Jwt jwt) {
        AccountEntity account = accounts.findBySubject(jwt.getSubject()).orElseGet(() -> provision(jwt));
        return response(account);
    }

    private AccountEntity provision(Jwt jwt) {
        UUID planId = plans.requireDefaultEntity().getId();
        Instant now = Instant.now();
        UUID id = jdbc.query("INSERT INTO accounts (subject, username, email, status, plan_id, created_at, updated_at) VALUES (?, ?, ?, 'ACTIVE', ?, ?, ?) ON CONFLICT (subject) DO NOTHING RETURNING id", ps -> {
            ps.setString(1, jwt.getSubject()); ps.setString(2, jwt.getClaimAsString("preferred_username")); ps.setString(3, jwt.getClaimAsString("email")); ps.setObject(4, planId); ps.setTimestamp(5, Timestamp.from(now)); ps.setTimestamp(6, Timestamp.from(now));
        }, rs -> rs.next() ? rs.getObject(1, UUID.class) : null);
        return accounts.findBySubject(jwt.getSubject()).orElseThrow();
    }

    public AccountResponse response(AccountEntity account) {
        var response = mapper.toResponse(account);
        return new AccountResponse(response.id(), response.subject(), response.username(), response.email(), response.status(), plans.find(account.getPlanId()));
    }

    public List<AccountResponse> all() { return accounts.findAllByOrderByCreatedAtAsc().stream().map(this::response).toList(); }
    @Transactional public AccountResponse block(UUID id, String actor, String reason) { return changeStatus(id, actor, reason, true); }
    @Transactional public AccountResponse unblock(UUID id, String actor, String reason) { return changeStatus(id, actor, reason, false); }
    private AccountResponse changeStatus(UUID id, String actor, String reason, boolean block) {
        var account = accounts.findById(id).orElseThrow(() -> new NotFoundException("Conta não encontrada"));
        if (block) account.block(); else account.unblock();
        accounts.save(account);
        audit(id, actor, block ? "ACCOUNT_BLOCKED" : "ACCOUNT_UNBLOCKED", reason);
        return response(account);
    }
    @Transactional public AccountResponse assignPlan(UUID id, UUID planId, String actor, String reason) {
        var plan = plans.requireActive(planId);
        var account = accounts.findById(id).orElseThrow(() -> new NotFoundException("Conta não encontrada"));
        account.assignPlan(plan.getId()); accounts.save(account); audit(id, actor, "PLAN_ASSIGNED", reason);
        return response(account);
    }
    private void audit(UUID id, String actor, String action, String reason) {
        jdbc.update("insert into account_audit (account_id, actor_subject, action, reason) values (?, ?, ?, ?)", id, actor, action, reason);
    }

    public static class NotFoundException extends RuntimeException { public NotFoundException(String message) { super(message); } }
    public static class InactivePlanException extends RuntimeException { public InactivePlanException() { super("Plano inativo não pode ser atribuído"); } }
}
