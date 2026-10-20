ALTER TABLE listings ADD COLUMN address_search_type VARCHAR(1);
ALTER TABLE listings ADD COLUMN address_search_selected_address VARCHAR(500);
ALTER TABLE listings ADD COLUMN address_search_jibun_address VARCHAR(500);
ALTER TABLE listings ADD COLUMN address_search_bcode VARCHAR(10);
ALTER TABLE listings ADD COLUMN address_search_sigungu_code VARCHAR(5);
ALTER TABLE listings ADD COLUMN address_search_no_selected VARCHAR(1);

COMMENT ON COLUMN listings.address_search_bcode IS '주소 검색 선택 시 받은 법정동 코드. 직접 입력·기존 매물은 NULL';
COMMENT ON COLUMN listings.address_search_jibun_address IS '주소 검색의 확정 지번. 자동 매핑 추정 주소는 저장하지 않음';
