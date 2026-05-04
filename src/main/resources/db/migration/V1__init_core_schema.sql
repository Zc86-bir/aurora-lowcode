-- V1__init_core_schema.sql
-- Aurora Low-Code Platform: Core database schema
-- Supports: PostgreSQL (primary) and MySQL (fallback)
-- Multi-tenancy: tenant_id required on all tables
-- Audit chain: prev_hash for SHA-256 hash chain integrity

-- ============================================================
-- 1. Tenant Management
-- ============================================================
CREATE TABLE IF NOT EXISTS tenant (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_code     VARCHAR(64)   NOT NULL UNIQUE,
    tenant_name     VARCHAR(128)  NOT NULL,
    tier            VARCHAR(32)   NOT NULL DEFAULT 'FREE',
    status          VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE',
    schema_name     VARCHAR(64),
    isolation_mode  VARCHAR(16)   NOT NULL DEFAULT 'SCHEMA',
    max_metadata    INT           NOT NULL DEFAULT 1000,
    max_skills      INT           NOT NULL DEFAULT 100,
    max_users       INT           NOT NULL DEFAULT 50,
    session_timeout INT           NOT NULL DEFAULT 1800,
    quota_metadata  INT           NOT NULL DEFAULT 0,
    quota_skills    INT           NOT NULL DEFAULT 0,
    quota_users     INT           NOT NULL DEFAULT 0,
    expires_at      TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ,
    created_by      VARCHAR(64)   NOT NULL DEFAULT 'system',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INT           NOT NULL DEFAULT 0
);

CREATE INDEX idx_tenant_code ON tenant(tenant_code);
CREATE INDEX idx_tenant_status ON tenant(status);

-- ============================================================
-- 2. User & Role
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID          NOT NULL,
    username        VARCHAR(64)   NOT NULL,
    password_hash   VARCHAR(256)  NOT NULL,
    display_name    VARCHAR(128),
    email           VARCHAR(128),
    phone           VARCHAR(32),
    status          VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE',
    login_attempts  INT           NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    last_login_at   TIMESTAMPTZ,
    force_password_change BOOLEAN NOT NULL DEFAULT false,
    created_by      VARCHAR(64)   NOT NULL DEFAULT 'system',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INT           NOT NULL DEFAULT 0,

    CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT uq_user_tenant_username UNIQUE (tenant_id, username)
);

CREATE INDEX idx_user_tenant ON sys_user(tenant_id);
CREATE INDEX idx_user_status ON sys_user(status);
CREATE INDEX idx_user_email ON sys_user(email);

CREATE TABLE IF NOT EXISTS sys_role (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID          NOT NULL,
    role_code       VARCHAR(64)   NOT NULL,
    role_name       VARCHAR(128)  NOT NULL,
    description     TEXT,
    permissions     JSONB,
    is_system       BOOLEAN       NOT NULL DEFAULT false,
    created_by      VARCHAR(64)   NOT NULL DEFAULT 'system',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INT           NOT NULL DEFAULT 0,

    CONSTRAINT fk_role_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT uq_role_tenant_code UNIQUE (tenant_id, role_code)
);

CREATE INDEX idx_role_tenant ON sys_role(tenant_id);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID          NOT NULL,
    role_id         UUID          NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES sys_role(id),
    CONSTRAINT uq_user_role UNIQUE (user_id, role_id)
);

CREATE INDEX idx_ur_user ON sys_user_role(user_id);
CREATE INDEX idx_ur_role ON sys_user_role(role_id);

-- ============================================================
-- 3. Metadata Registry
-- ============================================================
CREATE TABLE IF NOT EXISTS metadata (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID          NOT NULL,
    name            VARCHAR(128)  NOT NULL,
    type            VARCHAR(32)   NOT NULL,
    content         JSONB         NOT NULL,
    version         INT           NOT NULL DEFAULT 1,
    status          VARCHAR(16)   NOT NULL DEFAULT 'DRAFT',
    checksum_sha256 VARCHAR(64),
    parent_id       UUID,
    tags            TEXT[],
    created_by      VARCHAR(64)   NOT NULL DEFAULT 'system',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_metadata_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT uq_metadata_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_metadata_tenant ON metadata(tenant_id);
CREATE INDEX idx_metadata_type ON metadata(type);
CREATE INDEX idx_metadata_status ON metadata(status);
CREATE INDEX idx_metadata_parent ON metadata(parent_id);

CREATE TABLE IF NOT EXISTS metadata_version (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metadata_id     UUID          NOT NULL,
    version         INT           NOT NULL,
    content         JSONB         NOT NULL,
    checksum_sha256 VARCHAR(64)   NOT NULL,
    change_summary  TEXT,
    created_by      VARCHAR(64)   NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_mv_metadata FOREIGN KEY (metadata_id) REFERENCES metadata(id) ON DELETE CASCADE,
    CONSTRAINT uq_mv_metadata_version UNIQUE (metadata_id, version)
);

CREATE INDEX idx_mv_metadata ON metadata_version(metadata_id);

-- ============================================================
-- 4. Skill Registry
-- ============================================================
CREATE TABLE IF NOT EXISTS skill_registry (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID          NOT NULL,
    skill_id        VARCHAR(64)   NOT NULL,
    name            VARCHAR(128)  NOT NULL,
    version         VARCHAR(16)   NOT NULL DEFAULT '1.0.0',
    description     TEXT,
    category        VARCHAR(64),
    executor        VARCHAR(64),
    input_schema    JSONB,
    output_schema   JSONB,
    config          JSONB,
    status          VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE',
    deprecated      BOOLEAN       NOT NULL DEFAULT false,
    jeecg_compat    BOOLEAN       NOT NULL DEFAULT false,
    created_by      VARCHAR(64)   NOT NULL DEFAULT 'system',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_skill_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT uq_skill_tenant_skill_id UNIQUE (tenant_id, skill_id)
);

CREATE INDEX idx_skill_tenant ON skill_registry(tenant_id);
CREATE INDEX idx_skill_category ON skill_registry(category);
CREATE INDEX idx_skill_status ON skill_registry(status);

-- ============================================================
-- 5. Audit Log (SHA-256 Hash Chain)
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID          NOT NULL,
    action          VARCHAR(64)   NOT NULL,
    resource_type   VARCHAR(64)   NOT NULL,
    resource_id     VARCHAR(128),
    actor_id        VARCHAR(64)   NOT NULL,
    actor_type      VARCHAR(32)   NOT NULL DEFAULT 'user',
    details         JSONB,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(256),
    prev_hash       VARCHAR(64),
    entry_hash      VARCHAR(64)   NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

CREATE INDEX idx_audit_tenant ON audit_log(tenant_id);
CREATE INDEX idx_audit_action ON audit_log(action);
CREATE INDEX idx_audit_resource ON audit_log(resource_type, resource_id);
CREATE INDEX idx_audit_created_at ON audit_log(created_at);
CREATE INDEX idx_audit_entry_hash ON audit_log(entry_hash);

-- ============================================================
-- 6. ABAC Policy
-- ============================================================
CREATE TABLE IF NOT EXISTS abac_policy (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID          NOT NULL,
    policy_name     VARCHAR(128)  NOT NULL,
    policy_type     VARCHAR(32)   NOT NULL,
    subject_rules   JSONB,
    resource_rules  JSONB,
    action_rules    JSONB,
    env_rules       JSONB,
    effect          VARCHAR(8)    NOT NULL DEFAULT 'DENY',
    priority        INT           NOT NULL DEFAULT 0,
    enabled         BOOLEAN       NOT NULL DEFAULT true,
    created_by      VARCHAR(64)   NOT NULL DEFAULT 'system',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INT           NOT NULL DEFAULT 0,

    CONSTRAINT fk_policy_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

CREATE INDEX idx_policy_tenant ON abac_policy(tenant_id);
CREATE INDEX idx_policy_type ON abac_policy(policy_type);
CREATE INDEX idx_policy_priority ON abac_policy(priority);

-- ============================================================
-- 7. Skill Execution Log
-- ============================================================
CREATE TABLE IF NOT EXISTS skill_execution_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID          NOT NULL,
    skill_id        VARCHAR(64)   NOT NULL,
    execution_id    UUID          NOT NULL,
    status          VARCHAR(16)   NOT NULL DEFAULT 'RUNNING',
    input_payload   JSONB,
    output_payload  JSONB,
    error_message   TEXT,
    tokens_in       INT,
    tokens_out      INT,
    cost_usd        DECIMAL(10, 6),
    latency_ms      BIGINT,
    started_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    TIMESTAMPTZ,

    CONSTRAINT fk_exec_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

CREATE INDEX idx_exec_tenant ON skill_execution_log(tenant_id);
CREATE INDEX idx_exec_skill ON skill_execution_log(skill_id);
CREATE INDEX idx_exec_status ON skill_execution_log(status);
CREATE INDEX idx_exec_started ON skill_execution_log(started_at);

-- ============================================================
-- 8. Configuration
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_config (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID,
    config_key      VARCHAR(128)  NOT NULL,
    config_value    TEXT,
    config_type     VARCHAR(32)   NOT NULL DEFAULT 'STRING',
    scope           VARCHAR(32)   NOT NULL DEFAULT 'system',
    encrypted       BOOLEAN       NOT NULL DEFAULT false,
    description     TEXT,
    created_by      VARCHAR(64)   NOT NULL DEFAULT 'system',
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INT           NOT NULL DEFAULT 0,

    CONSTRAINT fk_config_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT uq_config_scope_key UNIQUE (scope, COALESCE(tenant_id, '00000000-0000-0000-0000-000000000000'::UUID), config_key)
);

CREATE INDEX idx_config_tenant ON sys_config(tenant_id);
CREATE INDEX idx_config_scope ON sys_config(scope);
