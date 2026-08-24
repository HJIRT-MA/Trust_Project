CREATE TABLE audit_findings (
    id BIGSERIAL PRIMARY KEY,
    smart_contract_id BIGINT NOT NULL,
    severity VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    code_snippet TEXT,
    validated_by_rules BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_smart_contract FOREIGN KEY(smart_contract_id) REFERENCES smart_contracts(id) ON DELETE CASCADE
);
