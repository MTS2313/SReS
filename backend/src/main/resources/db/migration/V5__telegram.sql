CREATE TABLE telegram_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL UNIQUE REFERENCES accounts (id),
    telegram_user_id BIGINT NOT NULL UNIQUE,
    chat_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE telegram_link_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts (id),
    code_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_telegram_link_codes_account ON telegram_link_codes (account_id, created_at DESC);

CREATE TABLE telegram_processed_updates (
    update_id BIGINT PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE telegram_conversations (
    telegram_user_id BIGINT PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts (id),
    chat_id BIGINT NOT NULL,
    state VARCHAR(30) NOT NULL CHECK (state IN ('TYPE_SELECTION', 'DESCRIPTION', 'PDF_OPTIONAL')),
    report_type VARCHAR(40),
    description TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE telegram_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID NOT NULL UNIQUE REFERENCES reports (id),
    account_id UUID NOT NULL REFERENCES accounts (id),
    telegram_user_id BIGINT NOT NULL,
    chat_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    next_attempt_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    last_error VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE telegram_delivery_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_id UUID NOT NULL REFERENCES telegram_deliveries (id),
    attempt_number INTEGER NOT NULL CHECK (attempt_number > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('FAILED', 'SUCCEEDED')),
    error_message VARCHAR(500),
    attempted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (delivery_id, attempt_number)
);

CREATE INDEX ix_telegram_deliveries_due ON telegram_deliveries (status, next_attempt_at);
