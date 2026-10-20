CREATE TABLE building_identifications (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    registry_snapshot_id BIGINT NOT NULL REFERENCES registry_snapshots(id),
    registry_document_id BIGINT NOT NULL REFERENCES registry_documents(id),
    execution_status VARCHAR(30) NOT NULL,
    resolution_status VARCHAR(30),
    reason VARCHAR(100),
    query_json TEXT,
    input_json TEXT NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    candidate_count INTEGER,
    management_number VARCHAR(100),
    parent_management_number VARCHAR(100),
    unit_name VARCHAR(100),
    source VARCHAR(50),
    queried_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_building_execution CHECK (execution_status IN
        ('PENDING', 'RUNNING', 'COMPLETED', 'INPUT_INCOMPLETE', 'INPUT_CONFLICT',
         'CALL_FAILED', 'DISABLED', 'SUPERSEDED')),
    CONSTRAINT chk_building_resolution CHECK (resolution_status IS NULL OR resolution_status IN
        ('MATCHED', 'NOT_FOUND', 'AMBIGUOUS', 'UNVERIFIABLE')),
    CONSTRAINT chk_building_attempt_count CHECK (attempt_count >= 0)
);
ALTER TABLE listings ADD COLUMN current_building_identification_id BIGINT
    REFERENCES building_identifications(id);
CREATE INDEX idx_building_identifications_listing ON building_identifications(listing_id, id DESC);
CREATE INDEX idx_building_identifications_execution ON building_identifications(execution_status);
