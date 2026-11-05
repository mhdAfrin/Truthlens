ALTER TABLE verification_records ADD COLUMN input_type VARCHAR(8) NOT NULL DEFAULT 'TEXT';
ALTER TABLE verification_records ADD COLUMN original_url VARCHAR(2048);
ALTER TABLE verification_records ADD COLUMN resolved_url VARCHAR(2048);
ALTER TABLE verification_records ADD CONSTRAINT verification_input_source CHECK (
 (input_type='TEXT' AND original_url IS NULL AND resolved_url IS NULL) OR
 (input_type='URL' AND original_url IS NOT NULL AND resolved_url IS NOT NULL)
);
