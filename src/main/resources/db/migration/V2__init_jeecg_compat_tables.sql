-- V2__init_jeecg_compat_tables.sql
-- JeecgBoot compatibility layer tables
-- Enables seamless migration from JeecgBoot to Aurora Low-Code Platform

-- ============================================================
-- 1. Jeecg Online Form Configuration
-- ============================================================
CREATE TABLE IF NOT EXISTS jeecg_onl_form (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID          NOT NULL,
    table_name          VARCHAR(128)  NOT NULL,
    form_name           VARCHAR(128)  NOT NULL,
    form_type           VARCHAR(32)   NOT NULL DEFAULT 'single',
    form_json           JSONB         NOT NULL,
    table_config        JSONB,
    button_config       JSONB,
    validation_config   JSONB,
    datasource_code     VARCHAR(64),
    status              VARCHAR(16)   NOT NULL DEFAULT 'DRAFT',
    is_published        BOOLEAN       NOT NULL DEFAULT false,
    published_at        TIMESTAMPTZ,
    checksum_sha256     VARCHAR(64),
    created_by          VARCHAR(64)   NOT NULL DEFAULT 'system',
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             INT           NOT NULL DEFAULT 0,

    CONSTRAINT fk_onl_form_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT uq_onl_form_tenant_table UNIQUE (tenant_id, table_name)
);

CREATE INDEX idx_onl_form_tenant ON jeecg_onl_form(tenant_id);
CREATE INDEX idx_onl_form_status ON jeecg_onl_form(status);
CREATE INDEX idx_onl_form_datasource ON jeecg_onl_form(datasource_code);

-- ============================================================
-- 2. Jeecg Online Report Configuration
-- ============================================================
CREATE TABLE IF NOT EXISTS jeecg_onl_report (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID          NOT NULL,
    report_code         VARCHAR(64)   NOT NULL,
    report_name         VARCHAR(128)  NOT NULL,
    report_type         VARCHAR(32)   NOT NULL DEFAULT 'table',
    report_json         JSONB         NOT NULL,
    query_sql           TEXT          NOT NULL,
    column_config       JSONB,
    filter_config       JSONB,
    export_formats      TEXT[],
    status              VARCHAR(16)   NOT NULL DEFAULT 'DRAFT',
    is_published        BOOLEAN       NOT NULL DEFAULT false,
    published_at        TIMESTAMPTZ,
    checksum_sha256     VARCHAR(64),
    created_by          VARCHAR(64)   NOT NULL DEFAULT 'system',
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             INT           NOT NULL DEFAULT 0,

    CONSTRAINT fk_onl_report_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT uq_onl_report_tenant_code UNIQUE (tenant_id, report_code)
);

CREATE INDEX idx_onl_report_tenant ON jeecg_onl_report(tenant_id);
CREATE INDEX idx_onl_report_type ON jeecg_onl_report(report_type);
CREATE INDEX idx_onl_report_status ON jeecg_onl_report(status);

-- ============================================================
-- 3. Jeecg BPMN Process Definition
-- ============================================================
CREATE TABLE IF NOT EXISTS jeecg_bpmn_process (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID          NOT NULL,
    process_key         VARCHAR(64)   NOT NULL,
    process_name        VARCHAR(128)  NOT NULL,
    bpmn_xml            TEXT          NOT NULL,
    process_version     INT           NOT NULL DEFAULT 1,
    category            VARCHAR(64),
    form_key            VARCHAR(64),
    description         TEXT,
    status              VARCHAR(16)   NOT NULL DEFAULT 'DRAFT',
    is_published        BOOLEAN       NOT NULL DEFAULT false,
    published_at        TIMESTAMPTZ,
    checksum_sha256     VARCHAR(64),
    created_by          VARCHAR(64)   NOT NULL DEFAULT 'system',
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             INT           NOT NULL DEFAULT 0,

    CONSTRAINT fk_bpmn_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT uq_bpmn_tenant_key_ver UNIQUE (tenant_id, process_key, process_version)
);

CREATE INDEX idx_bpmn_tenant ON jeecg_bpmn_process(tenant_id);
CREATE INDEX idx_bpmn_key ON jeecg_bpmn_process(process_key);
CREATE INDEX idx_bpmn_status ON jeecg_bpmn_process(status);
CREATE INDEX idx_bpmn_form_key ON jeecg_bpmn_process(form_key);

-- ============================================================
-- 4. Jeecg BPMN Process Instance
-- ============================================================
CREATE TABLE IF NOT EXISTS jeecg_bpmn_instance (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID          NOT NULL,
    process_id          UUID          NOT NULL,
    business_key        VARCHAR(128),
    title               VARCHAR(256),
    initiator           VARCHAR(64)   NOT NULL,
    current_node        VARCHAR(64),
    variables           JSONB,
    status              VARCHAR(16)   NOT NULL DEFAULT 'RUNNING',
    completed_at        TIMESTAMPTZ,
    created_by          VARCHAR(64)   NOT NULL DEFAULT 'system',
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_instance_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_instance_process FOREIGN KEY (process_id) REFERENCES jeecg_bpmn_process(id)
);

CREATE INDEX idx_instance_tenant ON jeecg_bpmn_instance(tenant_id);
CREATE INDEX idx_instance_process ON jeecg_bpmn_instance(process_id);
CREATE INDEX idx_instance_status ON jeecg_bpmn_instance(status);
CREATE INDEX idx_instance_initiator ON jeecg_bpmn_instance(initiator);
CREATE INDEX idx_instance_business_key ON jeecg_bpmn_instance(business_key);

-- ============================================================
-- 5. Jeecg DesForm (Designer Form JSON)
-- ============================================================
CREATE TABLE IF NOT EXISTS jeecg_desform (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID          NOT NULL,
    form_code           VARCHAR(64)   NOT NULL,
    form_name           VARCHAR(128)  NOT NULL,
    form_json           JSONB         NOT NULL,
    component_tree      JSONB,
    layout_config       JSONB,
    validation_rules    JSONB,
    status              VARCHAR(16)   NOT NULL DEFAULT 'DRAFT',
    is_published        BOOLEAN       NOT NULL DEFAULT false,
    published_at        TIMESTAMPTZ,
    checksum_sha256     VARCHAR(64),
    created_by          VARCHAR(64)   NOT NULL DEFAULT 'system',
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             INT           NOT NULL DEFAULT 0,

    CONSTRAINT fk_desform_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT uq_desform_tenant_code UNIQUE (tenant_id, form_code)
);

CREATE INDEX idx_desform_tenant ON jeecg_desform(tenant_id);
CREATE INDEX idx_desform_status ON jeecg_desform(status);
CREATE INDEX idx_desform_code ON jeecg_desform(form_code);

-- ============================================================
-- 6. JimuBI Dashboard
-- ============================================================
CREATE TABLE IF NOT EXISTS jimubi_dashboard (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID          NOT NULL,
    dashboard_code      VARCHAR(64)   NOT NULL,
    dashboard_name      VARCHAR(128)  NOT NULL,
    dashboard_json      JSONB         NOT NULL,
    widget_configs      JSONB,
    data_bindings       JSONB,
    layout_config       JSONB,
    refresh_interval    INT           NOT NULL DEFAULT 30,
    status              VARCHAR(16)   NOT NULL DEFAULT 'DRAFT',
    is_published        BOOLEAN       NOT NULL DEFAULT false,
    published_at        TIMESTAMPTZ,
    is_public           BOOLEAN       NOT NULL DEFAULT false,
    checksum_sha256     VARCHAR(64),
    created_by          VARCHAR(64)   NOT NULL DEFAULT 'system',
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             INT           NOT NULL DEFAULT 0,

    CONSTRAINT fk_dashboard_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT uq_dashboard_tenant_code UNIQUE (tenant_id, dashboard_code)
);

CREATE INDEX idx_dashboard_tenant ON jimubi_dashboard(tenant_id);
CREATE INDEX idx_dashboard_status ON jimubi_dashboard(status);

-- ============================================================
-- 7. JimuBI Big Screen
-- ============================================================
CREATE TABLE IF NOT EXISTS jimubi_bigscreen (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID          NOT NULL,
    screen_code         VARCHAR(64)   NOT NULL,
    screen_name         VARCHAR(128)  NOT NULL,
    bigscreen_json      JSONB         NOT NULL,
    component_configs   JSONB,
    data_streams        JSONB,
    animation_configs   JSONB,
    resolution_width    INT           NOT NULL DEFAULT 1920,
    resolution_height   INT           NOT NULL DEFAULT 1080,
    theme               VARCHAR(32)   NOT NULL DEFAULT 'dark',
    status              VARCHAR(16)   NOT NULL DEFAULT 'DRAFT',
    is_published        BOOLEAN       NOT NULL DEFAULT false,
    published_at        TIMESTAMPTZ,
    is_public           BOOLEAN       NOT NULL DEFAULT false,
    checksum_sha256     VARCHAR(64),
    created_by          VARCHAR(64)   NOT NULL DEFAULT 'system',
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             INT           NOT NULL DEFAULT 0,

    CONSTRAINT fk_bigscreen_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT uq_bigscreen_tenant_code UNIQUE (tenant_id, screen_code)
);

CREATE INDEX idx_bigscreen_tenant ON jimubi_bigscreen(tenant_id);
CREATE INDEX idx_bigscreen_status ON jimubi_bigscreen(status);

-- ============================================================
-- 8. JimuReport (积木报表)
-- ============================================================
CREATE TABLE IF NOT EXISTS jimureport_template (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID          NOT NULL,
    report_code         VARCHAR(64)   NOT NULL,
    report_name         VARCHAR(128)  NOT NULL,
    report_json         JSONB         NOT NULL,
    sql_query           TEXT          NOT NULL,
    column_definitions  JSONB,
    group_definitions   JSONB,
    header_footer       JSONB,
    page_orientation    VARCHAR(16)   NOT NULL DEFAULT 'portrait',
    page_size           VARCHAR(16)   NOT NULL DEFAULT 'A4',
    export_formats      TEXT[],
    status              VARCHAR(16)   NOT NULL DEFAULT 'DRAFT',
    is_published        BOOLEAN       NOT NULL DEFAULT false,
    published_at        TIMESTAMPTZ,
    checksum_sha256     VARCHAR(64),
    created_by          VARCHAR(64)   NOT NULL DEFAULT 'system',
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             INT           NOT NULL DEFAULT 0,

    CONSTRAINT fk_jimureport_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT uq_jimureport_tenant_code UNIQUE (tenant_id, report_code)
);

CREATE INDEX idx_jimureport_tenant ON jimureport_template(tenant_id);
CREATE INDEX idx_jimureport_status ON jimureport_template(status);
