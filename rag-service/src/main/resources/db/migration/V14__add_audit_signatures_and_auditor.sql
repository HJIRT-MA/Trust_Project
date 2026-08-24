ALTER TABLE smart_contracts ADD COLUMN auditor VARCHAR(255);

CREATE TABLE audit_report_signatures (
    id BIGSERIAL PRIMARY KEY,
    smart_contract_id BIGINT NOT NULL,
    pdf_hash VARCHAR(255) NOT NULL,
    signature TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_report_contract FOREIGN KEY (smart_contract_id) REFERENCES smart_contracts(id) ON DELETE CASCADE
);
