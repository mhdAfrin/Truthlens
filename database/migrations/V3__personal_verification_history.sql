CREATE TABLE verification_records (
 id UUID PRIMARY KEY,
 owner_id UUID NOT NULL REFERENCES user_accounts(id),
 title VARCHAR(180) NOT NULL,
 content TEXT NOT NULL,
 status VARCHAR(32) NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 report_json TEXT NOT NULL,
 CONSTRAINT verification_status_valid CHECK (status IN ('AWAITING_VERIFICATION','PROCESSING','COMPLETED','FAILED'))
);
CREATE INDEX verification_owner_date ON verification_records(owner_id, created_at DESC);
CREATE INDEX verification_owner_status_date ON verification_records(owner_id, status, created_at DESC);
