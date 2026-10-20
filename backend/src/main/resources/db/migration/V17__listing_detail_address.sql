ALTER TABLE listings ADD COLUMN detail_address VARCHAR(500);
-- Preserve existing full addresses; only move separate building/unit text when it is not already embedded.
UPDATE listings SET detail_address = CASE
    WHEN NULLIF(BTRIM(unit_number), '') IS NULL THEN ''
    WHEN POSITION(regexp_replace(unit_number, '[^가-힣A-Za-z0-9]', '', 'g') IN
                  regexp_replace(address, '[^가-힣A-Za-z0-9]', '', 'g')) > 0 THEN ''
    ELSE CONCAT_WS(' ', CASE WHEN NULLIF(BTRIM(building_name), '') IS NOT NULL
        AND POSITION(building_name IN address) = 0 THEN BTRIM(building_name) END, BTRIM(unit_number))
    END;
ALTER TABLE listings DROP COLUMN unit_number;
COMMENT ON COLUMN listings.address IS '사용자가 검색 또는 입력한 지번·도로명 기본 주소';
COMMENT ON COLUMN listings.detail_address IS '사용자가 입력한 상세주소. 기본 주소와 합쳐 등기부 주소 검증';
INSERT INTO code_values(group_id, code, code_name, description, sort_order, use_yn)
SELECT id, 'MULTI_FAMILY', '다가구주택', '다가구주택', 45, 'Y' FROM code_groups WHERE group_code='PROPERTY_TYPE'
ON CONFLICT DO NOTHING;
INSERT INTO code_values(group_id, code, code_name, description, sort_order, use_yn)
SELECT id, 'MULTI_UNIT', '다세대주택', '다세대주택', 46, 'Y' FROM code_groups WHERE group_code='PROPERTY_TYPE'
ON CONFLICT DO NOTHING;
