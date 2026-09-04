CREATE TABLE quota_allocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts (id),
    plan_id UUID NOT NULL REFERENCES plans (id),
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    total_units INTEGER NOT NULL CHECK (total_units >= 0),
    reserved_units INTEGER NOT NULL DEFAULT 0 CHECK (reserved_units >= 0),
    consumed_units INTEGER NOT NULL DEFAULT 0 CHECK (consumed_units >= 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'CLOSED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMPTZ,
    CHECK (period_end > period_start),
    CHECK (reserved_units + consumed_units <= total_units)
);

CREATE INDEX ix_quota_allocations_account_period ON quota_allocations (account_id, period_start);
CREATE UNIQUE INDEX ux_quota_allocations_one_active_period
    ON quota_allocations (account_id, period_start) WHERE status = 'ACTIVE';

CREATE TABLE quota_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts (id),
    allocation_id UUID NOT NULL REFERENCES quota_allocations (id),
    idempotency_key VARCHAR(255) NOT NULL,
    units INTEGER NOT NULL DEFAULT 1 CHECK (units > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('RESERVED', 'CONFIRMED', 'RELEASED')),
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '24 hours'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (account_id, idempotency_key)
);

CREATE INDEX ix_quota_reservations_allocation ON quota_reservations (allocation_id, status);

CREATE TABLE quota_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts (id),
    allocation_id UUID REFERENCES quota_allocations (id),
    reservation_id UUID REFERENCES quota_reservations (id),
    entry_type VARCHAR(30) NOT NULL CHECK (entry_type IN ('ALLOCATION', 'RESERVATION', 'CONSUMPTION', 'RELEASE', 'ADJUSTMENT', 'PLAN_RESET')),
    units_delta INTEGER NOT NULL,
    value_before INTEGER NOT NULL,
    value_after INTEGER NOT NULL,
    actor_subject VARCHAR(255),
    reason VARCHAR(500),
    plan_id UUID REFERENCES plans (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_quota_ledger_account_created ON quota_ledger (account_id, created_at);

CREATE TABLE usage_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts (id),
    allocation_id UUID REFERENCES quota_allocations (id),
    reservation_id UUID REFERENCES quota_reservations (id),
    model VARCHAR(255),
    input_tokens BIGINT,
    output_tokens BIGINT,
    total_tokens BIGINT NOT NULL CHECK (total_tokens >= 0),
    duration_ms BIGINT,
    attempts INTEGER NOT NULL DEFAULT 1 CHECK (attempts > 0),
    estimated_cost NUMERIC(19, 8) NOT NULL CHECK (estimated_cost >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_usage_metrics_account_created ON usage_metrics (account_id, created_at);
