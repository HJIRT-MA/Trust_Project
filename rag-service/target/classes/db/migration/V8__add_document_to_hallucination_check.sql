ALTER TABLE hallucination_checks ADD COLUMN document_id BIGINT;
ALTER TABLE hallucination_checks ADD CONSTRAINT fk_hc_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE;
