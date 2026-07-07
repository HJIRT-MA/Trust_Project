ALTER TABLE documents ADD COLUMN tenant_id VARCHAR(255) NOT NULL DEFAULT 'default_tenant';
ALTER TABLE chunks ADD COLUMN tenant_id VARCHAR(255) NOT NULL DEFAULT 'default_tenant';
ALTER TABLE chunks ADD COLUMN metadata JSONB;
