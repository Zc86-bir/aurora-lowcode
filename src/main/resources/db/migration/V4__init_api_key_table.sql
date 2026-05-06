-- V4: External API Key Management Table
-- Stores SHA-256 hashed API keys for external system integration.
-- Raw keys are NEVER stored; only shown once at creation time.

CREATE TABLE IF NOT EXISTS api_key (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    hashed_key      VARCHAR(64) NOT NULL UNIQUE,
    name            VARCHAR(128) NOT NULL,
    scopes          VARCHAR(256),
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    expires_at      TIMESTAMPTZ,
    last_used_at    TIMESTAMPTZ,
    created_by      VARCHAR(128) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version_lock    INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_apikey_tenant ON api_key(tenant_id);
CREATE INDEX IF NOT EXISTS idx_apikey_hash ON api_key(hashed_key);
