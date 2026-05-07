-- V3__init_sample_data.sql
-- Default tenant, admin user, and sample skill registrations

-- ============================================================
-- 1. Default Tenant
-- ============================================================
INSERT INTO tenant (id, tenant_code, tenant_name, tier, status, isolation_mode, max_metadata, max_skills, max_users)
VALUES ('00000000-0000-0000-0000-000000000001', 'default', 'Default Tenant', 'FREE', 'ACTIVE', 'SCHEMA', 1000, 100, 50)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 2. Admin User (password: "password" - must change on first login)
-- ============================================================
INSERT INTO sys_user (id, tenant_id, username, password_hash, display_name, email, status, force_password_change)
VALUES (
    '00000000-0000-0000-0000-000000000101',
    '00000000-0000-0000-0000-000000000001',
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'System Administrator',
    'admin@aurora.local',
    'ACTIVE',
    true
)
ON CONFLICT (tenant_id, username) DO NOTHING;

-- ============================================================
-- 3. System Roles
-- ============================================================
INSERT INTO sys_role (id, tenant_id, role_code, role_name, description, permissions, is_system)
VALUES
    ('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000001', 'admin', 'System Administrator', 'Full system access', '{"*": "*"}'::jsonb, true),
    ('00000000-0000-0000-0000-000000000202', '00000000-0000-0000-0000-000000000001', 'developer', 'Developer', 'Code generation and skill execution', '{"metadata": "rw", "skill": "rw", "codegen": "rw"}'::jsonb, true),
    ('00000000-0000-0000-0000-000000000203', '00000000-0000-0000-0000-000000000001', 'viewer', 'Read-Only Viewer', 'Read-only access to all resources', '{"metadata": "r", "skill": "r"}'::jsonb, true)
ON CONFLICT (tenant_id, role_code) DO NOTHING;

-- ============================================================
-- 4. Assign Admin Role to Admin User
-- ============================================================
INSERT INTO sys_user_role (id, user_id, role_id)
VALUES (
    '00000000-0000-0000-0000-000000000301',
    '00000000-0000-0000-0000-000000000101',
    '00000000-0000-0000-0000-000000000201'
)
ON CONFLICT (user_id, role_id) DO NOTHING;

-- ============================================================
-- 5. Register JeecgBoot Compatible Skills
-- ============================================================
INSERT INTO skill_registry (id, tenant_id, skill_id, name, version, description, category, executor, jeecg_compat, status)
VALUES
    ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'jeecg-codegen', 'Jeecg Code Generator', '1.0.0', 'Generate entity, controller, service, mapper code and DDL SQL', 'code_generation', 'ai-pipeline', true, 'ACTIVE'),
    ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'jeecg-bpmn', 'Jeecg BPMN Workflow Generator', '1.0.0', 'Generate BPMN 2.0 XML workflow definitions', 'workflow', 'ai-pipeline', true, 'ACTIVE'),
    ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'jeecg-onlform', 'Jeecg Online Form Designer', '1.0.0', 'Generate Online Form configuration JSON', 'form_design', 'ai-pipeline', true, 'ACTIVE'),
    ('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001', 'jeecg-onlreport', 'Jeecg Online Report Builder', '1.0.0', 'Generate Online Report configuration', 'report_design', 'ai-pipeline', true, 'ACTIVE'),
    ('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000001', 'jeecg-onlchart', 'Jeecg Online Chart Builder', '1.0.0', 'Generate online chart configurations', 'chart_design', 'ai-pipeline', true, 'ACTIVE'),
    ('10000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000001', 'jeecg-desform', 'Jeecg Designer Form JSON Generator', '1.0.0', 'Generate form JSON for DesForm', 'form_design', 'ai-pipeline', true, 'ACTIVE'),
    ('10000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000001', 'jeecg-system', 'Jeecg System Configuration Manager', '1.0.0', 'Manage system configuration with hot-reload', 'infrastructure', 'ai-pipeline', true, 'ACTIVE'),
    ('10000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000001', 'jimubi-dashboard', 'JimuBI Dashboard Designer', '1.0.0', 'Generate JimuBI dashboard configurations', 'dashboard_design', 'ai-pipeline', true, 'ACTIVE'),
    ('10000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000001', 'jimubi-bigscreen', 'JimuBI Big Screen Designer', '1.0.0', 'Generate JimuBI big screen configurations', 'bigscreen_design', 'ai-pipeline', true, 'ACTIVE'),
    ('10000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000001', 'jimureport', 'JimuReport Compatible Report Builder', '1.0.0', 'Generate JimuReport compatible report templates', 'report_design', 'ai-pipeline', true, 'ACTIVE')
ON CONFLICT (tenant_id, skill_id) DO NOTHING;

-- ============================================================
-- 6. Register Legacy Skills (backward compatible)
-- ============================================================
INSERT INTO skill_registry (id, tenant_id, skill_id, name, version, description, category, executor, status)
VALUES
    ('20000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'api_designer', 'API Designer', '1.0.0', 'Design REST API contracts from natural language', 'integration', 'ai-pipeline', 'ACTIVE'),
    ('20000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'permission_designer', 'Permission Designer', '1.0.0', 'Design RBAC/ABAC permission policies', 'security', 'ai-pipeline', 'ACTIVE'),
    ('20000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'template_generator', 'Template Generator', '1.0.0', 'Generate documentation and template files', 'document', 'ai-pipeline', 'ACTIVE')
ON CONFLICT (tenant_id, skill_id) DO NOTHING;

-- ============================================================
-- 7. Default System Configuration
-- ============================================================
INSERT INTO sys_config (id, config_key, config_value, config_type, scope, description)
VALUES
    ('30000000-0000-0000-0000-000000000001', 'platform.name', 'Aurora Low-Code Platform', 'STRING', 'system', 'Platform display name'),
    ('30000000-0000-0000-0000-000000000002', 'platform.version', '1.0.0', 'STRING', 'system', 'Platform version'),
    ('30000000-0000-0000-0000-000000000003', 'ai.pipeline.max-retries', '2', 'NUMBER', 'system', 'Maximum AI self-correction retry rounds'),
    ('30000000-0000-0000-0000-000000000004', 'ai.pipeline.timeout-seconds', '30', 'NUMBER', 'system', 'AI pipeline execution timeout in seconds'),
    ('30000000-0000-0000-0000-000000000005', 'skill.max-concurrent', '50', 'NUMBER', 'system', 'Maximum concurrent skill executions'),
    ('30000000-0000-0000-0000-000000000006', 'audit.hash-algorithm', 'SHA-256', 'STRING', 'system', 'Audit log hash chain algorithm'),
    ('30000000-0000-0000-0000-000000000007', 'tenancy.default-isolation', 'SCHEMA', 'STRING', 'system', 'Default tenant isolation mode')
ON CONFLICT (scope, COALESCE(tenant_id, '00000000-0000-0000-0000-000000000000'::UUID), config_key) DO NOTHING;

-- ============================================================
-- 8. Genesis Audit Entry (hash chain anchor)
-- ============================================================
INSERT INTO audit_log (id, tenant_id, user_id, action, resource_type, resource_id, details, entry_hash, prev_hash, trace_id, seq_num)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    NULL,
    'SYSTEM_INIT',
    'platform',
    'genesis',
    '{"message": "Platform initialization - genesis audit entry"}'::jsonb,
    'e3b0c44298fc1c149268a07a0d63a3b2748f4e0e31e8e4b8c3f5d6a7b8c9d0e1',
    NULL,
    'genesis',
    1
)
ON CONFLICT DO NOTHING;
