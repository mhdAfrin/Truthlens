ALTER TABLE verification_records ALTER COLUMN owner_id DROP NOT NULL;
ALTER TABLE verification_records ADD COLUMN guest_scope UUID;
ALTER TABLE verification_records ADD CONSTRAINT verification_access_scope CHECK (
 (owner_id IS NOT NULL AND guest_scope IS NULL) OR (owner_id IS NULL AND guest_scope IS NOT NULL)
);
CREATE INDEX verification_guest_scope ON verification_records(guest_scope, id);
