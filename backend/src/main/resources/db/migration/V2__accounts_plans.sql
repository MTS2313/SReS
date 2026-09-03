CREATE TABLE plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    weekly_limit INTEGER NOT NULL CHECK (weekly_limit > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX ux_plans_single_default ON plans (is_default) WHERE is_default = TRUE;

CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(100),
    email VARCHAR(255),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'BLOCKED')),
    plan_id UUID NOT NULL REFERENCES plans (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_accounts_status ON accounts (status);

CREATE TABLE account_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID REFERENCES accounts (id),
    actor_subject VARCHAR(255) NOT NULL,
    action VARCHAR(50) NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO plans (name, weekly_limit, active, is_default)
VALUES ('Plano Inicial', 10, TRUE, TRUE)
ON CONFLICT (name) DO NOTHING;
