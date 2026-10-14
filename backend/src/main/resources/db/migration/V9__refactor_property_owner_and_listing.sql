-- Stage 4.5: registry identity and snapshot-owned owner records.
-- Existing development data must be cleared before this migration is applied.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE properties
    ADD COLUMN registry_property_number VARCHAR(50) NOT NULL;
ALTER TABLE properties
    ADD CONSTRAINT uk_properties_registry_property_number UNIQUE (registry_property_number);

ALTER TABLE property_owners
    DROP CONSTRAINT uk_property_owners_property_user;
ALTER TABLE property_owners
    DROP CONSTRAINT fk_property_owners_user;
DROP INDEX idx_property_owners_user_id;
ALTER TABLE property_owners
    DROP COLUMN user_id;

ALTER TABLE property_owners
    ADD COLUMN registry_snapshot_id BIGINT NOT NULL,
    ADD COLUMN owner_name VARCHAR(200) NOT NULL,
    ADD COLUMN owner_rrn_encrypted BYTEA NOT NULL,
    ADD COLUMN owner_rrn_birth6_hash VARCHAR(64) NOT NULL,
    ADD COLUMN registered_at TIMESTAMP;

ALTER TABLE property_owners
    ADD CONSTRAINT fk_property_owners_registry_snapshot
        FOREIGN KEY (registry_snapshot_id) REFERENCES registry_snapshots(id);
ALTER TABLE registry_snapshots
    ADD CONSTRAINT uk_registry_snapshots_property_id_id UNIQUE (property_id, id);
ALTER TABLE property_owners
    ADD CONSTRAINT fk_property_owners_property_snapshot
        FOREIGN KEY (property_id, registry_snapshot_id)
        REFERENCES registry_snapshots(property_id, id);

CREATE INDEX idx_property_owners_registry_snapshot_id
    ON property_owners(registry_snapshot_id);

ALTER TABLE listings RENAME COLUMN seller_user_id TO registered_by_user_id;
ALTER TABLE listings RENAME CONSTRAINT fk_listings_seller TO fk_listings_registered_by_user;
ALTER INDEX idx_listings_seller_user_id RENAME TO idx_listings_registered_by_user_id;

COMMENT ON COLUMN properties.registry_property_number IS '등기부 부동산 고유번호';
COMMENT ON COLUMN property_owners.owner_rrn_encrypted IS '암호화된 등기부 소유자 식별정보';
COMMENT ON COLUMN property_owners.owner_rrn_birth6_hash IS '주민번호 앞 6자리 비교용 HMAC-SHA256';
COMMENT ON COLUMN listings.registered_by_user_id IS 'SafeHome에 매물을 등록한 사용자';
