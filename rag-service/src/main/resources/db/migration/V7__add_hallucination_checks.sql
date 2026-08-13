CREATE TABLE hallucination_checks (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    claim_text TEXT NOT NULL,
    status VARCHAR(255) NOT NULL,
    similarity_score DOUBLE PRECISION,
    source_chunks TEXT,
    CONSTRAINT fk_hallucination_check_message FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE
);
