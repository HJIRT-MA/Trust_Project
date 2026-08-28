CREATE TABLE blockchain_proofs (
    id SERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    tx_hash VARCHAR(255),
    block_number BIGINT,
    timestamp BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    user_id VARCHAR(255)
);

CREATE INDEX idx_blockchain_proofs_type ON blockchain_proofs(event_type);
CREATE INDEX idx_blockchain_proofs_status ON blockchain_proofs(status);
