ALTER TABLE property_owners ADD COLUMN owner_entity_type VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE property_owners ADD COLUMN corporate_registration_encrypted BYTEA;
ALTER TABLE property_owners ALTER COLUMN owner_rrn_encrypted DROP NOT NULL;
ALTER TABLE property_owners ALTER COLUMN owner_rrn_birth6_hash DROP NOT NULL;
ALTER TABLE property_owners ADD CONSTRAINT chk_owner_entity_type CHECK (owner_entity_type IN ('UNKNOWN','PERSON','CORPORATION'));
ALTER TABLE property_owners ADD CONSTRAINT chk_corporate_owner_identity CHECK (owner_entity_type <> 'CORPORATION' OR (owner_rrn_encrypted IS NULL AND owner_rrn_birth6_hash IS NULL));
