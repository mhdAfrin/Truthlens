ALTER TABLE verification_records DROP CONSTRAINT verification_status_valid;
ALTER TABLE verification_records ADD CONSTRAINT verification_status_valid CHECK (status IN ('QUEUED','AWAITING_VERIFICATION','PROCESSING','COMPLETED','FAILED'));
