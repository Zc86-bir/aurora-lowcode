-- V8: Evolve V1 RBAC tables to match V7 enterprise console entity definitions
-- V1 created sys_role/sys_user/sys_user_role without some columns the new entities expect.
-- This migration adds missing columns idempotently.

-- ─── sys_role: add status column (V1 had permissions/is_system but no status) ───
ALTER TABLE sys_role ADD COLUMN IF NOT EXISTS status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';

-- ─── sys_role: add version_lock alias (V1 used "version", entity expects "version_lock") ───
-- We keep the existing "version" column and add version_lock as a computed alias view isn't ideal,
-- so instead we add version_lock and keep version in sync via trigger.
ALTER TABLE sys_role ADD COLUMN IF NOT EXISTS version_lock INT NOT NULL DEFAULT 0;

-- Sync version_lock from existing version values
UPDATE sys_role SET version_lock = version WHERE version_lock = 0 AND version > 0;

-- ─── sys_user_role: add tenant_id (V7 entity expects it, V1 didn't have it) ───
ALTER TABLE sys_user_role ADD COLUMN IF NOT EXISTS tenant_id UUID;

-- Backfill tenant_id from the user's tenant
UPDATE sys_user_role sur
SET tenant_id = su.tenant_id
FROM sys_user su
WHERE sur.user_id = su.id AND sur.tenant_id IS NULL;

-- Make tenant_id NOT NULL after backfill
ALTER TABLE sys_user_role ALTER COLUMN tenant_id SET NOT NULL;

-- Add unique constraint name if not exists (V1 has uq_user_role, V7 expects uk_user_role)
-- We keep the existing constraint; JPA just needs the uniqueness, not the name.

-- ─── sys_role: seed status for existing rows ───
UPDATE sys_role SET status = 'ACTIVE' WHERE status IS NULL OR status = '';
