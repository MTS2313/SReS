package br.com.sres.usage;

import br.com.sres.accounts.AccountEntity;
import br.com.sres.accounts.AccountRepository;
import br.com.sres.accounts.AccountService;
import br.com.sres.accounts.AccountStatus;
import br.com.sres.plans.PlanResponse;
import br.com.sres.plans.PlanService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Service
public class QuotaService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    private final AccountRepository accounts;
    private final AccountService accountService;
    private final PlanService plans;
    private final JdbcTemplate jdbc;
    private final BigDecimal valuePerMillionTokens;

    public QuotaService(AccountRepository accounts, AccountService accountService, PlanService plans, JdbcTemplate jdbc,
                        @Value("${sres.cost.value-per-million-tokens:0}") BigDecimal valuePerMillionTokens) {
        this.accounts = accounts;
        this.accountService = accountService;
        this.plans = plans;
        this.jdbc = jdbc;
        this.valuePerMillionTokens = valuePerMillionTokens;
    }

    @Transactional
    public UsageSummary usage(Jwt jwt) {
        accountService.me(jwt);
        return usageForAccount(accountForSubject(jwt.getSubject()).getId());
    }

    @Transactional
    public UsageSummary usageForAccount(UUID accountId) {
        AccountRow allocation = ensureCurrentAllocation(accountId);
        return summary(allocation);
    }

    @Transactional(readOnly = true)
    public List<LedgerResponse> historyForAccount(UUID accountId) {
        accounts.findById(accountId).orElseThrow(() -> new NotFoundException("Conta não encontrada"));
        return jdbc.query("select id, entry_type, units_delta, value_before, value_after, actor_subject, reason, created_at from quota_ledger where account_id = ? order by created_at, id",
                (rs, row) -> new LedgerResponse(rs.getObject("id", UUID.class), rs.getString("entry_type"), rs.getInt("units_delta"), rs.getInt("value_before"), rs.getInt("value_after"), rs.getString("actor_subject"), rs.getString("reason"), rs.getTimestamp("created_at").toInstant()), accountId);
    }

    @Transactional
    public ReservationResponse reserve(Jwt jwt, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key é obrigatório");
        }
        accountService.me(jwt);
        AccountEntity account = accountForSubject(jwt.getSubject());
        return reserveForAccount(account.getId(), idempotencyKey, jwt.getSubject());
    }

    @Transactional
    public ReservationResponse reserveForAccount(UUID accountId, String idempotencyKey, String actor) {
        AccountEntity account = accounts.findById(accountId).orElseThrow(() -> new NotFoundException("Conta não encontrada"));
        if (account.getStatus() == AccountStatus.BLOCKED) throw new BlockedAccountException();
        AccountRow allocation = lockAllocation(ensureCurrentAllocation(account.getId()).id());
        ReservationRow existing = findReservation(account.getId(), idempotencyKey);
        if (existing != null) {
            if (existing.expiresAt().isAfter(Instant.now())) return new ReservationResponse(existing.id(), existing.units(), existing.status());
            jdbc.update("update quota_reservations set idempotency_key = concat(idempotency_key, ':expired:', id::text), updated_at = current_timestamp where id = ?", existing.id());
        }
        int available = allocation.total() - allocation.reserved() - allocation.consumed();
        if (available < 1) throw new QuotaExceededException();
        UUID id = jdbc.queryForObject("insert into quota_reservations (account_id, allocation_id, idempotency_key, units, status) values (?, ?, ?, 1, 'RESERVED') returning id",
                UUID.class, account.getId(), allocation.id(), idempotencyKey);
        jdbc.update("update quota_allocations set reserved_units = reserved_units + 1 where id = ?", allocation.id());
        ledger(account.getId(), allocation.id(), id, "RESERVATION", 0, available, available, actor, null, allocation.planId());
        return new ReservationResponse(id, 1, "RESERVED");
    }

    @Transactional
    public ReservationResponse confirm(Jwt jwt, UUID reservationId) {
        ReservationContext context = lockReservation(reservationId, jwt.getSubject());
        if (!"RESERVED".equals(context.status())) return new ReservationResponse(reservationId, context.units(), context.status());
        AccountRow allocation = lockAllocation(context.allocationId());
        int before = allocation.consumed();
        jdbc.update("update quota_reservations set status = 'CONFIRMED', updated_at = current_timestamp where id = ?", reservationId);
        jdbc.update("update quota_allocations set reserved_units = reserved_units - 1, consumed_units = consumed_units + 1 where id = ?", allocation.id());
        ledger(context.accountId(), allocation.id(), reservationId, "CONSUMPTION", 1, before, before + 1, jwt.getSubject(), null, allocation.planId());
        return new ReservationResponse(reservationId, context.units(), "CONFIRMED");
    }

    @Transactional
    public ReservationResponse release(Jwt jwt, UUID reservationId) {
        ReservationContext context = lockReservation(reservationId, jwt.getSubject());
        if (!"RESERVED".equals(context.status())) return new ReservationResponse(reservationId, context.units(), context.status());
        AccountRow allocation = lockAllocation(context.allocationId());
        int availableBefore = allocation.total() - allocation.reserved() - allocation.consumed();
        jdbc.update("update quota_reservations set status = 'RELEASED', updated_at = current_timestamp where id = ?", reservationId);
        jdbc.update("update quota_allocations set reserved_units = reserved_units - 1 where id = ?", allocation.id());
        ledger(context.accountId(), allocation.id(), reservationId, "RELEASE", 0, availableBefore, availableBefore + 1, jwt.getSubject(), null, allocation.planId());
        return new ReservationResponse(reservationId, context.units(), "RELEASED");
    }

    @Transactional
    public void confirmForWorker(UUID reservationId, UUID accountId) {
        ReservationContext context = lockReservationForAccount(reservationId, accountId);
        if (!"RESERVED".equals(context.status())) return;
        AccountRow allocation = lockAllocation(context.allocationId());
        int before = allocation.consumed();
        jdbc.update("update quota_reservations set status = 'CONFIRMED', updated_at = current_timestamp where id = ?", reservationId);
        jdbc.update("update quota_allocations set reserved_units = reserved_units - 1, consumed_units = consumed_units + 1 where id = ?", allocation.id());
        ledger(context.accountId(), allocation.id(), reservationId, "CONSUMPTION", 1, before, before + 1, "worker", null, allocation.planId());
    }

    @Transactional
    public void releaseForWorker(UUID reservationId, UUID accountId) {
        ReservationContext context = lockReservationForAccount(reservationId, accountId);
        if (!"RESERVED".equals(context.status())) return;
        AccountRow allocation = lockAllocation(context.allocationId());
        int availableBefore = allocation.total() - allocation.reserved() - allocation.consumed();
        jdbc.update("update quota_reservations set status = 'RELEASED', updated_at = current_timestamp where id = ?", reservationId);
        jdbc.update("update quota_allocations set reserved_units = reserved_units - 1 where id = ?", allocation.id());
        ledger(context.accountId(), allocation.id(), reservationId, "RELEASE", 0, availableBefore, availableBefore + 1, "worker", null, allocation.planId());
    }

    @Transactional
    public void releaseForAccount(UUID reservationId, UUID accountId) {
        ReservationContext context = lockReservationForAccount(reservationId, accountId);
        if (!"RESERVED".equals(context.status())) return;
        AccountRow allocation = lockAllocation(context.allocationId());
        int availableBefore = allocation.total() - allocation.reserved() - allocation.consumed();
        jdbc.update("update quota_reservations set status = 'RELEASED', updated_at = current_timestamp where id = ?", reservationId);
        jdbc.update("update quota_allocations set reserved_units = reserved_units - 1 where id = ?", allocation.id());
        ledger(context.accountId(), allocation.id(), reservationId, "RELEASE", 0, availableBefore, availableBefore + 1, "telegram", null, allocation.planId());
    }

    @Transactional
    public UsageSummary adjust(UUID accountId, int units, String actor, String reason) {
        requireReason(reason);
        AccountRow allocation = lockAllocation(ensureCurrentAllocation(accountId).id());
        int before = allocation.total();
        int after = before + units;
        if (after < allocation.reserved() + allocation.consumed()) throw new ConflictException("Ajuste reduziria a cota já utilizada");
        jdbc.update("update quota_allocations set total_units = ? where id = ?", after, allocation.id());
        ledger(accountId, allocation.id(), null, "ADJUSTMENT", units, before, after, actor, reason, allocation.planId());
        return summary(lockAllocation(allocation.id()));
    }

    @Transactional
    public UsageSummary changePlan(UUID accountId, UUID planId, String actor, String reason) {
        AccountEntity account = accounts.findById(accountId).orElseThrow(() -> new NotFoundException("Conta não encontrada"));
        plans.requireActive(planId);
        PlanResponse plan = plans.find(planId);
        requireReason(reason);
        lockAccount(accountId);
        AccountRow previous = lockAllocation(ensureCurrentAllocation(accountId).id());
        jdbc.update("update quota_allocations set status = 'CLOSED', closed_at = current_timestamp where id = ?", previous.id());
        jdbc.update("update accounts set plan_id = ?, updated_at = current_timestamp where id = ?", plan.id(), accountId);
        AccountRow next = insertAllocation(accountId, plan.id(), plan.weeklyLimit(), currentPeriodStart(), currentPeriodStart().plusDays(7), "PLAN_RESET", actor, reason, previous.total());
        return summary(next);
    }

    @Transactional
    public int renewAll() {
        int renewed = 0;
        for (AccountEntity account : accounts.findAll()) {
            ensureCurrentAllocation(account.getId());
            renewed++;
        }
        return renewed;
    }

    @Transactional(readOnly = true)
    public List<LedgerResponse> history(Jwt jwt) {
        accountService.me(jwt);
        UUID accountId = accountForSubject(jwt.getSubject()).getId();
        return jdbc.query("select id, entry_type, units_delta, value_before, value_after, actor_subject, reason, created_at from quota_ledger where account_id = ? order by created_at, id",
                (rs, row) -> new LedgerResponse(rs.getObject("id", UUID.class), rs.getString("entry_type"), rs.getInt("units_delta"), rs.getInt("value_before"), rs.getInt("value_after"), rs.getString("actor_subject"), rs.getString("reason"), rs.getTimestamp("created_at").toInstant()), accountId);
    }

    @Transactional
    public MetricResponse recordMetric(UUID accountId, MetricRequest request) {
        AccountEntity account = accounts.findById(accountId).orElseThrow(() -> new NotFoundException("Conta não encontrada"));
        AccountRow allocation = ensureCurrentAllocation(account.getId());
        long input = request.inputTokens() == null ? 0 : request.inputTokens();
        long output = request.outputTokens() == null ? 0 : request.outputTokens();
        long total = input + output;
        BigDecimal cost = BigDecimal.valueOf(total).multiply(valuePerMillionTokens).divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);
        UUID id = jdbc.queryForObject("insert into usage_metrics (account_id, allocation_id, reservation_id, model, input_tokens, output_tokens, total_tokens, duration_ms, attempts, estimated_cost) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) returning id",
                UUID.class, accountId, allocation.id(), request.reservationId(), request.model(), input, output, total, request.durationMs() == null ? 0 : request.durationMs(), request.attempts() == null ? 1 : request.attempts(), cost);
        return metric(id, accountId, request.model(), input, output, total, request.durationMs(), request.attempts(), cost);
    }

    @Transactional(readOnly = true)
    public List<MetricResponse> costs() {
        return jdbc.query("select id, account_id, model, input_tokens, output_tokens, total_tokens, duration_ms, attempts, estimated_cost, created_at from usage_metrics order by created_at, id", (rs, row) -> metric(rs));
    }

    private AccountEntity accountForSubject(String subject) {
        return accounts.findBySubject(subject).orElseThrow(() -> new NotFoundException("Conta não encontrada"));
    }

    private AccountRow ensureCurrentAllocation(UUID accountId) {
        LocalDate start = currentPeriodStart();
        lockAccount(accountId);
        List<AccountRow> rows = jdbc.query("select id, account_id, plan_id, total_units, reserved_units, consumed_units, period_start, period_end, status from quota_allocations where account_id = ? and period_start = ? and status = 'ACTIVE'", (rs, row) -> allocation(rs, row), accountId, start);
        if (!rows.isEmpty()) return rows.getFirst();
        AccountEntity account = accounts.findById(accountId).orElseThrow(() -> new NotFoundException("Conta não encontrada"));
        PlanResponse plan = plans.find(account.getPlanId());
        jdbc.update("update quota_allocations set status = 'CLOSED', closed_at = current_timestamp where account_id = ? and status = 'ACTIVE'", accountId);
        return insertAllocation(accountId, plan.id(), plan.weeklyLimit(), start, start.plusDays(7), "ALLOCATION", null, "Plano", 0);
    }

    private AccountRow insertAllocation(UUID accountId, UUID planId, int total, LocalDate start, LocalDate end, String type, String actor, String reason, int oldValue) {
        UUID id = jdbc.queryForObject("insert into quota_allocations (account_id, plan_id, period_start, period_end, total_units, status) values (?, ?, ?, ?, ?, 'ACTIVE') returning id", UUID.class, accountId, planId, start, end, total);
        ledger(accountId, id, null, type, total - oldValue, oldValue, total, actor, reason, planId);
        return lockAllocation(id);
    }

    private AccountRow lockAllocation(UUID id) {
        return jdbc.queryForObject("select id, account_id, plan_id, total_units, reserved_units, consumed_units, period_start, period_end, status from quota_allocations where id = ? for update", (rs, row) -> allocation(rs, row), id);
    }

    private ReservationRow findReservation(UUID accountId, String key) {
        List<ReservationRow> rows = jdbc.query("select id, units, status, expires_at from quota_reservations where account_id = ? and idempotency_key = ? for update", (rs, row) -> new ReservationRow(rs.getObject("id", UUID.class), rs.getInt("units"), rs.getString("status"), rs.getTimestamp("expires_at").toInstant()), accountId, key);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private ReservationContext lockReservation(UUID id, String subject) {
        ReservationContext context = jdbc.queryForObject("select r.id, r.allocation_id, a.account_id, r.units, r.status from quota_reservations r join quota_allocations a on a.id = r.allocation_id where r.id = ? for update", (rs, row) -> new ReservationContext(rs.getObject("id", UUID.class), rs.getObject("allocation_id", UUID.class), rs.getObject("account_id", UUID.class), rs.getInt("units"), rs.getString("status")), id);
        AccountEntity account = accountForSubject(subject);
        if (!account.getId().equals(context.accountId())) throw new NotFoundException("Reserva não encontrada");
        return context;
    }

    private ReservationContext lockReservationForAccount(UUID id, UUID accountId) {
        ReservationContext context = jdbc.queryForObject("select r.id, r.allocation_id, a.account_id, r.units, r.status from quota_reservations r join quota_allocations a on a.id = r.allocation_id where r.id = ? for update", (rs, row) -> new ReservationContext(rs.getObject("id", UUID.class), rs.getObject("allocation_id", UUID.class), rs.getObject("account_id", UUID.class), rs.getInt("units"), rs.getString("status")), id);
        if (!accountId.equals(context.accountId())) throw new NotFoundException("Reserva não encontrada");
        return context;
    }

    private UsageSummary summary(AccountRow row) {
        return new UsageSummary(row.accountId(), row.id(), row.planId(), row.total(), row.total() - row.reserved() - row.consumed(), row.reserved(), row.consumed(), row.periodEnd());
    }

    private void ledger(UUID accountId, UUID allocationId, UUID reservationId, String type, int delta, int before, int after, String actor, String reason, UUID planId) {
        jdbc.update("insert into quota_ledger (account_id, allocation_id, reservation_id, entry_type, units_delta, value_before, value_after, actor_subject, reason, plan_id) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", accountId, allocationId, reservationId, type, delta, before, after, actor, reason, planId);
    }

    private MetricResponse metric(ResultSet rs) throws java.sql.SQLException {
        return metric(rs.getObject("id", UUID.class), rs.getObject("account_id", UUID.class), rs.getString("model"), rs.getLong("input_tokens"), rs.getLong("output_tokens"), rs.getLong("total_tokens"), rs.getLong("duration_ms"), rs.getInt("attempts"), rs.getBigDecimal("estimated_cost"), rs.getTimestamp("created_at").toInstant());
    }

    private MetricResponse metric(UUID id, UUID accountId, String model, long input, long output, long total, Long duration, Integer attempts, BigDecimal cost) {
        return metric(id, accountId, model, input, output, total, duration == null ? 0 : duration, attempts == null ? 1 : attempts, cost, Instant.now());
    }

    private MetricResponse metric(UUID id, UUID accountId, String model, long input, long output, long total, long duration, int attempts, BigDecimal cost, Instant created) {
        return new MetricResponse(id, accountId, model, input, output, total, duration, attempts, cost, created);
    }

    private static AccountRow allocation(ResultSet rs, int row) throws java.sql.SQLException {
        return new AccountRow(rs.getObject("id", UUID.class), rs.getObject("account_id", UUID.class), rs.getObject("plan_id", UUID.class), rs.getInt("total_units"), rs.getInt("reserved_units"), rs.getInt("consumed_units"), rs.getObject("period_start", LocalDate.class), rs.getObject("period_end", LocalDate.class), rs.getString("status"));
    }

    private static LocalDate currentPeriodStart() {
        return LocalDate.now(BUSINESS_ZONE).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
    }

    private static void requireReason(String reason) { if (reason == null || reason.isBlank()) throw new BadRequestException("Motivo é obrigatório"); }

    private void lockAccount(UUID accountId) {
        jdbc.query("select pg_advisory_xact_lock(hashtextextended(?, 0))",
                ps -> ps.setString(1, accountId.toString()), rs -> null);
    }

    private record AccountRow(UUID id, UUID accountId, UUID planId, int total, int reserved, int consumed, LocalDate periodStart, LocalDate periodEnd, String status) { }
    private record ReservationRow(UUID id, int units, String status, Instant expiresAt) { }
    private record ReservationContext(UUID id, UUID allocationId, UUID accountId, int units, String status) { }

    public static class NotFoundException extends RuntimeException { public NotFoundException(String message) { super(message); } }
    public static class BadRequestException extends RuntimeException { public BadRequestException(String message) { super(message); } }
    public static class ConflictException extends RuntimeException { public ConflictException(String message) { super(message); } }
    public static class QuotaExceededException extends RuntimeException { public QuotaExceededException() { super("Cota esgotada"); } }
    public static class BlockedAccountException extends RuntimeException { public BlockedAccountException() { super("Conta bloqueada não pode iniciar consumo"); } }
}
