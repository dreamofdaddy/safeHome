-- User-initiated registry analysis. Upload remains a separate, immediate operation.
INSERT INTO code_values(group_id, code, code_name, description, sort_order, use_yn)
SELECT id, 'REGISTRY_PROCESSING', '등기부 판독 중', '사용자가 요청한 등기부 PDF 비동기 판독 중', 6, 'Y'
FROM code_groups WHERE group_code = 'LISTING_STATUS';

INSERT INTO code_values(group_id, code, code_name, description, sort_order, use_yn)
SELECT id, 'REGISTRY_READY', '등기부 판독 완료', '등기부 구조화 완료, 안전성분석 대기', 7, 'Y'
FROM code_groups WHERE group_code = 'LISTING_STATUS';

INSERT INTO code_values(group_id, code, code_name, description, sort_order, use_yn)
SELECT id, 'REGISTRY_FAILED', '등기부 판독 실패', '등기부 판독 실패, 재시도 가능', 8, 'Y'
FROM code_groups WHERE group_code = 'LISTING_STATUS';

INSERT INTO code_values(group_id, code, code_name, description, sort_order, use_yn)
SELECT id, 'UPLOADED_PDF', '사용자 업로드 등기부', '사용자가 올린 등기사항증명서 PDF', 20, 'Y'
FROM code_groups WHERE group_code = 'REGISTRY_SOURCE';

ALTER TABLE registry_documents
    ADD COLUMN parser_provider VARCHAR(30),
    ADD COLUMN parser_model VARCHAR(100);

CREATE INDEX idx_registry_documents_listing_id ON registry_documents(listing_id);
