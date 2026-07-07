CREATE TABLE interaction_log (
    id SERIAL PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255),
    query_text TEXT,
    tokens_used INTEGER NOT NULL DEFAULT 0,
    model_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_interaction_log_tenant ON interaction_log(tenant_id);
CREATE INDEX idx_interaction_log_created_at ON interaction_log(created_at);
