ALTER TABLE audit_findings
ADD COLUMN swc_id VARCHAR(50),
ADD COLUMN swc_title VARCHAR(255),
ADD COLUMN enriched_explanation TEXT,
ADD COLUMN vulnerable_example TEXT;

ALTER TABLE smart_contracts
ADD COLUMN global_risk_score INTEGER,
ADD COLUMN risk_level VARCHAR(50);
