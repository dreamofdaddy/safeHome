INSERT INTO code_values(group_id, code, code_name, description, sort_order, use_yn)
SELECT id, 'SAFETY_ANALYZED', '안전성 분석 완료', '등기부 기반 안전성 분석 및 결과 저장 완료', 9, 'Y'
FROM code_groups WHERE group_code = 'LISTING_STATUS';

ALTER TABLE listings
    ADD COLUMN risk_analysis_id BIGINT,
    ADD CONSTRAINT fk_listings_risk_analysis
        FOREIGN KEY (risk_analysis_id) REFERENCES risk_analyses(id);

CREATE INDEX idx_listings_risk_analysis_id ON listings(risk_analysis_id);
