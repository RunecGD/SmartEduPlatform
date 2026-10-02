CREATE TABLE IF NOT EXISTS smartedu_jobs (
    id UUID PRIMARY KEY,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('EVENT','INDEX')),
    routing_key VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_smartedu_jobs_next ON smartedu_jobs(next_at);
