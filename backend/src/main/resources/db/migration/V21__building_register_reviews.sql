CREATE TABLE building_register_reviews (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    owner_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('UPLOADED','PROCESSING','PARSED','FAILED','SUPERSEDED')),
    files_json TEXT NOT NULL,
    context_json TEXT NOT NULL,
    result_json TEXT,
    error_code VARCHAR(80),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count>=0),
    consented_at TIMESTAMPTZ,
    parser_provider VARCHAR(30),
    parser_model VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_building_register_reviews_listing ON building_register_reviews(listing_id,id DESC);
COMMENT ON TABLE building_register_reviews IS '소유자 전용 건축물대장 첨부 묶음·입력 근거·판독 이력. 기존 등기·위험 점수와 분리';
