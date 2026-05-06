-- V5: Webhook Engine Tables
-- Stores webhook endpoint configurations and event delivery metadata.

CREATE TABLE IF NOT EXISTS webhook_endpoint (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID NOT NULL,
    url                  VARCHAR(2048) NOT NULL,
    secret               VARCHAR(128) NOT NULL,
    events               VARCHAR(512),
    active               BOOLEAN NOT NULL DEFAULT TRUE,
    description          VARCHAR(256),
    retry_count          INTEGER NOT NULL DEFAULT 0,
    success_count        BIGINT NOT NULL DEFAULT 0,
    failure_count        BIGINT NOT NULL DEFAULT 0,
    last_delivered_at    TIMESTAMPTZ,
    last_failure_at      TIMESTAMPTZ,
    last_failure_message VARCHAR(1024),
    created_by           VARCHAR(128) NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    version_lock         INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_webhook_tenant ON webhook_endpoint(tenant_id);
CREATE INDEX IF NOT EXISTS idx_webhook_active ON webhook_endpoint(active);
