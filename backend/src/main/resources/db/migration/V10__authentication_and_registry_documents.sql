ALTER TABLE users ADD COLUMN password_hash VARCHAR(100);

-- A listing can be submitted before the registry PDF is analyzed and linked to a Property.
ALTER TABLE listings ALTER COLUMN property_id DROP NOT NULL;
ALTER TABLE listings ADD COLUMN listing_address VARCHAR(500);
ALTER TABLE listings ADD COLUMN road_address VARCHAR(500);
ALTER TABLE listings ADD COLUMN building_name VARCHAR(200);
ALTER TABLE listings ADD COLUMN building_type VARCHAR(50);
ALTER TABLE listings ADD COLUMN unit_number VARCHAR(100);
ALTER TABLE listings ADD COLUMN postal_code VARCHAR(20);

INSERT INTO code_values(group_id, code, code_name, description, sort_order, use_yn)
SELECT id, 'READY_FOR_ANALYSIS', '분석 대기', '등기부 업로드 완료, 사용자의 안전성분석 요청 대기', 5, 'Y'
FROM code_groups WHERE group_code = 'LISTING_STATUS';

CREATE TABLE auth_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_auth_tokens_user_id ON auth_tokens(user_id);

CREATE TABLE registry_documents (
    id BIGSERIAL PRIMARY KEY,
    uploaded_by_user_id BIGINT NOT NULL REFERENCES users(id),
    property_id BIGINT REFERENCES properties(id),
    registry_snapshot_id BIGINT REFERENCES registry_snapshots(id),
    listing_id BIGINT REFERENCES listings(id),
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    encrypted_content BYTEA NOT NULL,
    status VARCHAR(30) NOT NULL,
    parsed_registry_property_number VARCHAR(50),
    error_code VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);
CREATE INDEX idx_registry_documents_user_id ON registry_documents(uploaded_by_user_id);
CREATE INDEX idx_registry_documents_status ON registry_documents(status);
