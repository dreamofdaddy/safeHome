-- Keep existing snapshots/files and shared property records when a listing is deleted.
ALTER TABLE listings ADD COLUMN deleted_at TIMESTAMP;
CREATE INDEX idx_listings_not_deleted ON listings(registered_by_user_id) WHERE deleted_at IS NULL;
ALTER TABLE risk_analyses ADD COLUMN listing_input_fingerprint VARCHAR(64);
ALTER TABLE property_owners ADD COLUMN ownership_share VARCHAR(100);
ALTER TABLE property_owners ALTER COLUMN ownership_ratio TYPE NUMERIC(12,8);
COMMENT ON COLUMN property_owners.ownership_share IS 'Original printed share fraction; percentage is calculated separately';
COMMENT ON COLUMN listings.deleted_at IS 'Owner deletion hides the listing while retaining private history pending retention policy';
