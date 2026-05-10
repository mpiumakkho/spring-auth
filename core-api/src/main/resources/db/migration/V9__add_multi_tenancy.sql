-- Multi-tenancy: scope users/roles/audit/notifications to a tenant.
-- Permissions remain global (shared catalog).

CREATE TABLE IF NOT EXISTS sample_app.tenants (
    tenant_id   VARCHAR(255) PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    status      VARCHAR(20)  NOT NULL DEFAULT 'active',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Default tenant for backfilling existing rows
INSERT INTO sample_app.tenants (tenant_id, name, slug, status)
VALUES ('default-tenant', 'Default Tenant', 'default', 'active')
ON CONFLICT (tenant_id) DO NOTHING;

-- Add tenant_id to user-scoped tables
ALTER TABLE sample_app.users         ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255);
ALTER TABLE sample_app.roles         ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255);
ALTER TABLE sample_app.audit_logs    ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255);
ALTER TABLE sample_app.notifications ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255);

-- Backfill existing rows to default tenant
UPDATE sample_app.users         SET tenant_id = 'default-tenant' WHERE tenant_id IS NULL;
UPDATE sample_app.roles         SET tenant_id = 'default-tenant' WHERE tenant_id IS NULL;
UPDATE sample_app.audit_logs    SET tenant_id = 'default-tenant' WHERE tenant_id IS NULL;
UPDATE sample_app.notifications SET tenant_id = 'default-tenant' WHERE tenant_id IS NULL;

-- Lock the column NOT NULL and add FKs
ALTER TABLE sample_app.users         ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE sample_app.roles         ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE sample_app.audit_logs    ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE sample_app.notifications ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE sample_app.users
    ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES sample_app.tenants(tenant_id);
ALTER TABLE sample_app.roles
    ADD CONSTRAINT fk_roles_tenant FOREIGN KEY (tenant_id) REFERENCES sample_app.tenants(tenant_id);
ALTER TABLE sample_app.audit_logs
    ADD CONSTRAINT fk_audit_logs_tenant FOREIGN KEY (tenant_id) REFERENCES sample_app.tenants(tenant_id);
ALTER TABLE sample_app.notifications
    ADD CONSTRAINT fk_notifications_tenant FOREIGN KEY (tenant_id) REFERENCES sample_app.tenants(tenant_id);

-- Tenant-scoped indexes
CREATE INDEX IF NOT EXISTS idx_users_tenant_id         ON sample_app.users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_roles_tenant_id         ON sample_app.roles(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_id    ON sample_app.audit_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_notifications_tenant_id ON sample_app.notifications(tenant_id);
