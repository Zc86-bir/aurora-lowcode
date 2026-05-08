-- V7: Enterprise Console — AI model configs + RBAC tables
-- Adds tables for AI model configuration and RBAC (users, roles, menus, buttons, data rules)

-- ─── AI Model Config ───

CREATE TABLE IF NOT EXISTS ai_model_config (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    model_id        VARCHAR(128) NOT NULL,
    api_key_encrypted VARCHAR(512),
    request_url     VARCHAR(512),
    display_name    VARCHAR(128),
    provider        VARCHAR(64),
    status          VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    is_default      BOOLEAN NOT NULL DEFAULT false,
    created_by      VARCHAR(128) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(128),
    updated_at      TIMESTAMPTZ,
    version_lock    INT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_aimodel_tenant ON ai_model_config(tenant_id);
CREATE INDEX IF NOT EXISTS idx_aimodel_model_id ON ai_model_config(model_id);

-- ─── RBAC: Users ───

CREATE TABLE IF NOT EXISTS sys_user (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    username        VARCHAR(128) NOT NULL,
    email           VARCHAR(128),
    phone           VARCHAR(64),
    password_hash   VARCHAR(256),
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_by      VARCHAR(128),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(128),
    updated_at      TIMESTAMPTZ,
    version_lock    INT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_sysuser_tenant ON sys_user(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sysuser_email ON sys_user(email);

-- ─── RBAC: Roles ───

CREATE TABLE IF NOT EXISTS sys_role (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    role_code       VARCHAR(64) NOT NULL,
    role_name       VARCHAR(128) NOT NULL,
    description     VARCHAR(512),
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_by      VARCHAR(128),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(128),
    updated_at      TIMESTAMPTZ,
    version_lock    INT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_sysrole_tenant ON sys_role(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sysrole_code ON sys_role(role_code);

-- ─── RBAC: Menus ───

CREATE TABLE IF NOT EXISTS sys_menu (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    name            VARCHAR(128) NOT NULL,
    path            VARCHAR(256),
    parent_id       UUID,
    sort_order      INT,
    icon            VARCHAR(128),
    type            VARCHAR(16) NOT NULL DEFAULT 'MENU',
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    permission_key  VARCHAR(128),
    created_by      VARCHAR(128),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(128),
    updated_at      TIMESTAMPTZ,
    version_lock    INT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_sysmenu_tenant ON sys_menu(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sysmenu_parent ON sys_menu(parent_id);

-- ─── RBAC: Button Permissions ───

CREATE TABLE IF NOT EXISTS sys_button_permission (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    menu_id         UUID NOT NULL,
    button_code     VARCHAR(128) NOT NULL,
    button_name     VARCHAR(128) NOT NULL,
    permission_key  VARCHAR(128) NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_by      VARCHAR(128),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version_lock    INT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_sysbtn_tenant ON sys_button_permission(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sysbtn_menu ON sys_button_permission(menu_id);

-- ─── RBAC: Data Rules ───

CREATE TABLE IF NOT EXISTS sys_data_rule (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    role_id         UUID NOT NULL,
    resource_type   VARCHAR(64) NOT NULL,
    rule_name       VARCHAR(128) NOT NULL,
    rule_expression JSONB NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_by      VARCHAR(128),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(128),
    updated_at      TIMESTAMPTZ,
    version_lock    INT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_sysdatarule_tenant ON sys_data_rule(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sysdatarule_role ON sys_data_rule(role_id);

-- ─── RBAC: Join Tables ───

CREATE TABLE IF NOT EXISTS sys_user_role (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    role_id         UUID NOT NULL,
    tenant_id       UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id         UUID NOT NULL,
    menu_id         UUID NOT NULL,
    tenant_id       UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_role_menu UNIQUE (role_id, menu_id)
);

-- ─── Seed Data ───

-- Default roles
INSERT INTO sys_role (id, tenant_id, role_code, role_name, description, created_by, created_at)
VALUES
    ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000001', 'ADMIN', 'System Administrator', 'Full system access', 'system', NOW()),
    ('00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000001', 'USER', 'Standard User', 'Basic user access', 'system', NOW())
ON CONFLICT DO NOTHING;

-- Default menus for enterprise console
INSERT INTO sys_menu (id, tenant_id, name, path, parent_id, sort_order, icon, type, created_by, created_at)
VALUES
    ('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000001', 'System', '', NULL, 100, 'SettingOutlined', 'GROUP', 'system', NOW()),
    ('00000000-0000-0000-0000-000000000202', '00000000-0000-0000-0000-000000000001', 'User Management', '/system/users', '00000000-0000-0000-0000-000000000201', 101, 'UserOutlined', 'MENU', 'system', NOW()),
    ('00000000-0000-0000-0000-000000000203', '00000000-0000-0000-0000-000000000001', 'Role Management', '/system/roles', '00000000-0000-0000-0000-000000000201', 102, 'TeamOutlined', 'MENU', 'system', NOW()),
    ('00000000-0000-0000-0000-000000000204', '00000000-0000-0000-0000-000000000001', 'Menu Management', '/system/menus', '00000000-0000-0000-0000-000000000201', 103, 'MenuOutlined', 'MENU', 'system', NOW()),
    ('00000000-0000-0000-0000-000000000205', '00000000-0000-0000-0000-000000000001', 'Button Permissions', '/system/buttons', '00000000-0000-0000-0000-000000000201', 104, 'KeyOutlined', 'MENU', 'system', NOW()),
    ('00000000-0000-0000-0000-000000000206', '00000000-0000-0000-0000-000000000001', 'Data Rules', '/system/data-rules', '00000000-0000-0000-0000-000000000201', 105, 'DatabaseOutlined', 'MENU', 'system', NOW()),
    ('00000000-0000-0000-0000-000000000207', '00000000-0000-0000-0000-000000000001', 'AI Model Config', '/ai/models', NULL, 200, 'RobotOutlined', 'MENU', 'system', NOW())
ON CONFLICT DO NOTHING;

-- Assign all menus to ADMIN role
INSERT INTO sys_role_menu (id, role_id, menu_id, tenant_id)
SELECT
    gen_random_uuid(),
    '00000000-0000-0000-0000-000000000101',
    id,
    '00000000-0000-0000-0000-000000000001'
FROM sys_menu
WHERE tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT DO NOTHING;
