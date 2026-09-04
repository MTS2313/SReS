CREATE TABLE reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts (id),
    report_type VARCHAR(40) NOT NULL CHECK (report_type IN ('EXECUTIVE_SUMMARY', 'DETAILED_ANALYSIS', 'STRUCTURED_EXTRACTION')),
    origin VARCHAR(20) NOT NULL CHECK (origin IN ('API', 'TELEGRAM')),
    description TEXT NOT NULL,
    extracted_text TEXT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    reservation_id UUID NOT NULL UNIQUE REFERENCES quota_reservations (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_reports_account_created ON reports (account_id, created_at DESC);
CREATE INDEX ix_reports_status_created ON reports (status, created_at);

CREATE TABLE report_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID NOT NULL REFERENCES reports (id),
    file_kind VARCHAR(20) NOT NULL CHECK (file_kind IN ('INPUT_PDF', 'OUTPUT_MARKDOWN')),
    bucket_name VARCHAR(255) NOT NULL,
    object_key VARCHAR(1000) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes >= 0),
    temporary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (report_id, file_kind),
    UNIQUE (bucket_name, object_key)
);

CREATE TABLE report_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID NOT NULL REFERENCES reports (id),
    attempt_number INTEGER NOT NULL CHECK (attempt_number > 0),
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    UNIQUE (report_id, attempt_number)
);

CREATE TABLE report_idempotency (
    account_id UUID NOT NULL REFERENCES accounts (id),
    idempotency_key VARCHAR(255) NOT NULL,
    report_id UUID NOT NULL REFERENCES reports (id),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (account_id, idempotency_key),
    UNIQUE (report_id)
);
