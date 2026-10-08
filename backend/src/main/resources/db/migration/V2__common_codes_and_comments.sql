-- ============================================================
-- SafeHome V2
-- Common Code Tables + Database Comments
-- ============================================================

-- ============================================================
-- 1. Common Code Group
-- ============================================================

CREATE TABLE code_groups (
    id          BIGSERIAL PRIMARY KEY,
    group_code  VARCHAR(50)  NOT NULL,
    group_name  VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    use_yn      CHAR(1)      NOT NULL DEFAULT 'Y',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_code_groups_group_code
        UNIQUE (group_code),

    CONSTRAINT chk_code_groups_use_yn
        CHECK (use_yn IN ('Y', 'N'))
);

CREATE INDEX idx_code_groups_sort_order
    ON code_groups(sort_order);


COMMENT ON TABLE code_groups IS
'SafeHome 공통 코드의 업무 그룹을 관리하는 기준정보 테이블';

COMMENT ON COLUMN code_groups.id IS
'코드 그룹 식별자';

COMMENT ON COLUMN code_groups.group_code IS
'코드 그룹의 시스템 식별 코드. 코드값을 구분하는 상위 업무 영역';

COMMENT ON COLUMN code_groups.group_name IS
'코드 그룹의 업무 명칭';

COMMENT ON COLUMN code_groups.description IS
'코드 그룹의 업무적 의미와 적용 범위';

COMMENT ON COLUMN code_groups.sort_order IS
'코드 그룹 표시 및 관리 순서';

COMMENT ON COLUMN code_groups.use_yn IS
'코드 그룹 사용 여부. Y=사용, N=미사용';

COMMENT ON COLUMN code_groups.created_at IS
'코드 그룹 생성 일시';

COMMENT ON COLUMN code_groups.updated_at IS
'코드 그룹 수정 일시';


-- ============================================================
-- 2. Common Code Value
-- ============================================================

CREATE TABLE code_values (
    id          BIGSERIAL PRIMARY KEY,
    group_id    BIGINT       NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    code_name   VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    use_yn      CHAR(1)      NOT NULL DEFAULT 'Y',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_code_values_group_code
        UNIQUE (group_id, code),

    CONSTRAINT fk_code_values_group
        FOREIGN KEY (group_id)
        REFERENCES code_groups(id),

    CONSTRAINT chk_code_values_use_yn
        CHECK (use_yn IN ('Y', 'N'))
);

CREATE INDEX idx_code_values_group_id
    ON code_values(group_id);

CREATE INDEX idx_code_values_sort_order
    ON code_values(group_id, sort_order);


COMMENT ON TABLE code_values IS
'SafeHome 공통 코드의 실제 코드값과 명칭 및 설명을 관리하는 기준정보 테이블';

COMMENT ON COLUMN code_values.id IS
'코드값 식별자';

COMMENT ON COLUMN code_values.group_id IS
'상위 코드 그룹 식별자';

COMMENT ON COLUMN code_values.code IS
'애플리케이션과 데이터에서 사용하는 안정적인 시스템 코드값';

COMMENT ON COLUMN code_values.code_name IS
'사용자 화면 및 문서에서 표시할 코드 명칭';

COMMENT ON COLUMN code_values.description IS
'코드값의 업무적 의미와 적용 기준';

COMMENT ON COLUMN code_values.sort_order IS
'동일 코드 그룹 내 표시 및 처리 순서';

COMMENT ON COLUMN code_values.use_yn IS
'코드값 사용 여부. Y=사용, N=미사용';

COMMENT ON COLUMN code_values.created_at IS
'코드값 생성 일시';

COMMENT ON COLUMN code_values.updated_at IS
'코드값 수정 일시';


-- ============================================================
-- 3. Code Groups
-- ============================================================

INSERT INTO code_groups
    (group_code, group_name, description, sort_order)
VALUES
    ('USER_VERIFICATION', '사용자 인증 상태',
     '사용자의 본인확인 및 인증 처리 상태를 관리한다', 10),

    ('USER_STATUS', '사용자 상태',
     '사용자 계정의 업무상 상태를 관리한다', 20),

    ('PROPERTY_TYPE', '부동산 유형',
     '부동산의 건물 및 주택 유형을 관리한다', 30),

    ('REGISTRY_SOURCE', '등기정보 출처',
     '등기부 관련 데이터의 수집 또는 생성 출처를 관리한다', 40),

    ('REGISTRY_RIGHT_TYPE', '등기 권리 유형',
     '등기부 스냅샷에서 확인되는 권리관계의 유형을 관리한다', 50),

    ('REGISTRY_RIGHT_STATUS', '등기 권리 상태',
     '등기 권리의 현재 유효 상태를 관리한다', 60),

    ('LISTING_TRANSACTION', '매물 거래 유형',
     '매물의 거래 방식을 관리한다', 70),

    ('LISTING_STATUS', '매물 상태',
     '매물의 등록 및 거래 진행 상태를 관리한다', 80),

    ('CONTRACT_TYPE', '계약 유형',
     '부동산 계약의 거래 유형을 관리한다', 90),

    ('CONTRACT_STATUS', '계약 상태',
     '계약의 업무 진행 상태를 관리한다', 100),

    ('CONTRACT_TERM_TYPE', '계약 특약 유형',
     '계약 특약의 업무 유형을 관리한다', 110),

    ('RISK_LEVEL', '위험 등급',
     '부동산 안전성 분석 결과의 종합 위험 수준을 관리한다', 120),

    ('RISK_FACTOR_TYPE', '위험 요인 유형',
     '부동산 안전성 분석에서 식별되는 개별 위험 요인의 유형을 관리한다', 130),

    ('RISK_SEVERITY', '위험 요인 심각도',
     '개별 위험 요인의 영향 수준을 관리한다', 140);


-- ============================================================
-- 4. Code Values
-- ============================================================

-- ------------------------------------------------------------
-- USER_VERIFICATION
-- ------------------------------------------------------------

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'UNVERIFIED', '미인증',
       '사용자 본인확인이 완료되지 않은 상태', 10
FROM code_groups
WHERE group_code = 'USER_VERIFICATION';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'VERIFIED', '인증 완료',
       '사용자 본인확인이 완료된 상태', 20
FROM code_groups
WHERE group_code = 'USER_VERIFICATION';


-- ------------------------------------------------------------
-- USER_STATUS
-- ------------------------------------------------------------

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'ACTIVE', '활성',
       '정상적으로 서비스를 이용할 수 있는 사용자 상태', 10
FROM code_groups
WHERE group_code = 'USER_STATUS';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'INACTIVE', '비활성',
       '사용이 중단된 사용자 상태', 20
FROM code_groups
WHERE group_code = 'USER_STATUS';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'BLOCKED', '차단',
       '정책 또는 업무상 사유로 사용이 제한된 사용자 상태', 30
FROM code_groups
WHERE group_code = 'USER_STATUS';


-- ------------------------------------------------------------
-- PROPERTY_TYPE
-- ------------------------------------------------------------

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'APARTMENT', '아파트',
       '공동주택 중 아파트 유형', 10
FROM code_groups
WHERE group_code = 'PROPERTY_TYPE';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'OFFICETEL', '오피스텔',
       '업무시설 및 주거 용도로 사용되는 오피스텔 유형', 20
FROM code_groups
WHERE group_code = 'PROPERTY_TYPE';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'VILLA', '빌라',
       '다세대 또는 연립 형태의 주거 유형', 30
FROM code_groups
WHERE group_code = 'PROPERTY_TYPE';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'DETACHED_HOUSE', '단독주택',
       '독립된 건물 형태의 단독주택 유형', 40
FROM code_groups
WHERE group_code = 'PROPERTY_TYPE';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'OTHER', '기타',
       '사전에 정의되지 않은 부동산 유형', 90
FROM code_groups
WHERE group_code = 'PROPERTY_TYPE';


-- ------------------------------------------------------------
-- REGISTRY_SOURCE
-- ------------------------------------------------------------

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'TEST', '테스트',
       '개발 및 검증을 위해 생성된 테스트 등기정보', 10
FROM code_groups
WHERE group_code = 'REGISTRY_SOURCE';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'OTHER', '기타',
       '별도로 정의되지 않은 등기정보 출처', 90
FROM code_groups
WHERE group_code = 'REGISTRY_SOURCE';


-- ------------------------------------------------------------
-- REGISTRY_RIGHT_TYPE
-- ------------------------------------------------------------

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'MORTGAGE', '근저당권',
       '부동산을 담보로 설정된 저당권 관련 권리', 10
FROM code_groups
WHERE group_code = 'REGISTRY_RIGHT_TYPE';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'LEASEHOLD', '전세권',
       '부동산에 설정된 전세권 관련 권리', 20
FROM code_groups
WHERE group_code = 'REGISTRY_RIGHT_TYPE';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'SEIZURE', '압류',
       '채권 확보 등을 목적으로 설정된 압류 관련 권리', 30
FROM code_groups
WHERE group_code = 'REGISTRY_RIGHT_TYPE';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'PROVISIONAL_SEIZURE', '가압류',
       '본안 집행에 앞서 재산 처분을 제한하기 위한 가압류 관련 권리', 40
FROM code_groups
WHERE group_code = 'REGISTRY_RIGHT_TYPE';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'OTHER', '기타',
       '사전에 정의되지 않은 등기 권리 유형', 90
FROM code_groups
WHERE group_code = 'REGISTRY_RIGHT_TYPE';


-- ------------------------------------------------------------
-- REGISTRY_RIGHT_STATUS
-- ------------------------------------------------------------

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'ACTIVE', '유효',
       '현재 유효한 권리 상태', 10
FROM code_groups
WHERE group_code = 'REGISTRY_RIGHT_STATUS';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'INACTIVE', '비활성',
       '현재 유효하지 않거나 효력이 종료된 권리 상태', 20
FROM code_groups
WHERE group_code = 'REGISTRY_RIGHT_STATUS';


-- ------------------------------------------------------------
-- LISTING_TRANSACTION
-- ------------------------------------------------------------

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'JEONSE', '전세',
       '보증금을 중심으로 거래하는 임대차 방식', 10
FROM code_groups
WHERE group_code = 'LISTING_TRANSACTION';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'MONTHLY_RENT', '월세',
       '보증금과 월 차임을 함께 지급하는 임대차 방식', 20
FROM code_groups
WHERE group_code = 'LISTING_TRANSACTION';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'SALE', '매매',
       '부동산 소유권 이전을 전제로 하는 매매 방식', 30
FROM code_groups
WHERE group_code = 'LISTING_TRANSACTION';


-- ------------------------------------------------------------
-- LISTING_STATUS
-- ------------------------------------------------------------

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'ACTIVE', '판매 중',
       '현재 유효하게 노출되는 매물 상태', 10
FROM code_groups
WHERE group_code = 'LISTING_STATUS';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'EXPIRED', '만료',
       '게시 유효기간이 종료된 매물 상태', 20
FROM code_groups
WHERE group_code = 'LISTING_STATUS';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'CLOSED', '종료',
       '거래 또는 게시가 종료된 매물 상태', 30
FROM code_groups
WHERE group_code = 'LISTING_STATUS';


-- ------------------------------------------------------------
-- CONTRACT_TYPE
-- ------------------------------------------------------------

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'JEONSE', '전세',
       '전세 방식의 임대차 계약', 10
FROM code_groups
WHERE group_code = 'CONTRACT_TYPE';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'MONTHLY_RENT', '월세',
       '월세 방식의 임대차 계약', 20
FROM code_groups
WHERE group_code = 'CONTRACT_TYPE';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'SALE', '매매',
       '매매 방식의 부동산 계약', 30
FROM code_groups
WHERE group_code = 'CONTRACT_TYPE';


-- ------------------------------------------------------------
-- CONTRACT_STATUS
-- ------------------------------------------------------------

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'DRAFT', '작성 중',
       '계약이 작성되었으나 최종 체결되지 않은 상태', 10
FROM code_groups
WHERE group_code = 'CONTRACT_STATUS';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'ACTIVE', '진행 중',
       '계약이 체결되어 유효하게 진행 중인 상태', 20
FROM code_groups
WHERE group_code = 'CONTRACT_STATUS';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'COMPLETED', '종료',
       '계약 기간 또는 계약 업무가 정상적으로 종료된 상태', 30
FROM code_groups
WHERE group_code = 'CONTRACT_STATUS';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'CANCELLED', '취소',
       '계약이 취소되거나 효력을 상실한 상태', 40
FROM code_groups
WHERE group_code = 'CONTRACT_STATUS';


-- ------------------------------------------------------------
-- CONTRACT_TERM_TYPE
-- ------------------------------------------------------------

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'PAYMENT', '지급',
       '보증금, 차임, 잔금 등 지급 조건에 관한 특약', 10
FROM code_groups
WHERE group_code = 'CONTRACT_TERM_TYPE';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'REPAIR', '수리',
       '시설물 유지보수 및 수리 책임에 관한 특약', 20
FROM code_groups
WHERE group_code = 'CONTRACT_TERM_TYPE';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'OTHER', '기타',
       '사전에 정의되지 않은 계약 특약 유형', 90
FROM code_groups
WHERE group_code = 'CONTRACT_TERM_TYPE';


-- ------------------------------------------------------------
-- RISK_LEVEL
-- ------------------------------------------------------------

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'LOW', '낮음',
       '식별된 위험 수준이 상대적으로 낮은 상태', 10
FROM code_groups
WHERE group_code = 'RISK_LEVEL';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'MEDIUM', '보통',
       '주의가 필요한 중간 수준의 위험 상태', 20
FROM code_groups
WHERE group_code = 'RISK_LEVEL';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'HIGH', '높음',
       '거래 안전성 측면에서 높은 주의가 필요한 상태', 30
FROM code_groups
WHERE group_code = 'RISK_LEVEL';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'CRITICAL', '매우 높음',
       '거래 안전성 측면에서 중대한 위험이 식별된 상태', 40
FROM code_groups
WHERE group_code = 'RISK_LEVEL';


-- ------------------------------------------------------------
-- RISK_FACTOR_TYPE
-- ------------------------------------------------------------

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'MORTGAGE', '근저당권',
       '근저당권 설정에 따른 위험 요인', 10
FROM code_groups
WHERE group_code = 'RISK_FACTOR_TYPE';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'OWNERSHIP_MISMATCH', '소유자 불일치',
       '등기상 소유자와 거래 당사자 정보가 일치하지 않는 위험 요인', 20
FROM code_groups
WHERE group_code = 'RISK_FACTOR_TYPE';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'PRIORITY_RIGHT', '선순위 권리',
       '거래 대상보다 우선하는 권리관계에 따른 위험 요인', 30
FROM code_groups
WHERE group_code = 'RISK_FACTOR_TYPE';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'SEIZURE', '압류',
       '압류 또는 가압류와 관련된 위험 요인', 40
FROM code_groups
WHERE group_code = 'RISK_FACTOR_TYPE';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'OTHER', '기타',
       '사전에 정의되지 않은 위험 요인 유형', 90
FROM code_groups
WHERE group_code = 'RISK_FACTOR_TYPE';


-- ------------------------------------------------------------
-- RISK_SEVERITY
-- ------------------------------------------------------------

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'LOW', '낮음',
       '개별 위험 요인의 영향도가 낮은 상태', 10
FROM code_groups
WHERE group_code = 'RISK_SEVERITY';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'MEDIUM', '보통',
       '개별 위험 요인의 영향도가 중간 수준인 상태', 20
FROM code_groups
WHERE group_code = 'RISK_SEVERITY';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'HIGH', '높음',
       '개별 위험 요인의 영향도가 높은 상태', 30
FROM code_groups
WHERE group_code = 'RISK_SEVERITY';

INSERT INTO code_values
    (group_id, code, code_name, description, sort_order)
SELECT id, 'CRITICAL', '매우 높음',
       '개별 위험 요인의 영향도가 매우 높은 상태', 40
FROM code_groups
WHERE group_code = 'RISK_SEVERITY';


-- ============================================================
-- 5. Existing Table Comments
-- ============================================================

COMMENT ON TABLE users IS
'SafeHome 플랫폼 사용자 계정과 사용자 인증 및 상태를 관리하는 테이블';

COMMENT ON TABLE properties IS
'SafeHome 거래 대상이 되는 부동산의 기본 식별 및 주소 정보를 관리하는 테이블';

COMMENT ON TABLE property_owners IS
'부동산과 사용자 간의 소유관계 및 지분 정보를 관리하는 테이블';

COMMENT ON TABLE registry_snapshots IS
'특정 시점에 관찰한 부동산 등기정보의 스냅샷을 관리하는 테이블';

COMMENT ON TABLE registry_rights IS
'특정 등기정보 스냅샷에서 확인된 권리관계를 관리하는 테이블';

COMMENT ON TABLE listings IS
'부동산의 매물 등록 정보와 거래 조건 및 게시 상태를 관리하는 테이블';

COMMENT ON TABLE contracts IS
'부동산 거래 당사자 간의 계약 조건과 계약 기간 및 상태를 관리하는 테이블';

COMMENT ON TABLE contract_special_terms IS
'부동산 계약에 포함되는 특약의 유형과 상세 내용을 관리하는 테이블';

COMMENT ON TABLE risk_analyses IS
'부동산의 안전성을 분석한 결과와 분석 버전을 관리하는 테이블';

COMMENT ON TABLE risk_factors IS
'부동산 안전성 분석에서 식별된 개별 위험 요인을 관리하는 테이블';


-- ============================================================
-- 6. users Column Comments
-- ============================================================

COMMENT ON COLUMN users.id IS
'사용자 식별자';

COMMENT ON COLUMN users.email IS
'사용자 로그인 및 연락 식별에 사용하는 이메일 주소';

COMMENT ON COLUMN users.name IS
'사용자의 실명 또는 서비스상 사용자명';

COMMENT ON COLUMN users.phone IS
'사용자 연락처';

COMMENT ON COLUMN users.verification_status IS
'사용자 본인확인 상태 코드. USER_VERIFICATION 코드 그룹을 사용한다';

COMMENT ON COLUMN users.status IS
'사용자 계정 상태 코드. USER_STATUS 코드 그룹을 사용한다';

COMMENT ON COLUMN users.created_at IS
'사용자 계정 생성 일시';

COMMENT ON COLUMN users.updated_at IS
'사용자 계정 최종 수정 일시';


-- ============================================================
-- 7. properties Column Comments
-- ============================================================

COMMENT ON COLUMN properties.id IS
'부동산 식별자';

COMMENT ON COLUMN properties.address IS
'부동산의 기본 주소 정보';

COMMENT ON COLUMN properties.road_address IS
'도로명 주소';

COMMENT ON COLUMN properties.building_name IS
'건물 또는 단지 명칭';

COMMENT ON COLUMN properties.building_type IS
'부동산 건물 유형 코드. PROPERTY_TYPE 코드 그룹을 사용한다';

COMMENT ON COLUMN properties.unit_number IS
'동, 층, 호 등 대상 세대 식별 정보';

COMMENT ON COLUMN properties.postal_code IS
'우편번호';

COMMENT ON COLUMN properties.created_at IS
'부동산 정보 생성 일시';

COMMENT ON COLUMN properties.updated_at IS
'부동산 정보 최종 수정 일시';


-- ============================================================
-- 8. property_owners Column Comments
-- ============================================================

COMMENT ON COLUMN property_owners.id IS
'부동산 소유관계 식별자';

COMMENT ON COLUMN property_owners.property_id IS
'소유관계 대상 부동산 식별자';

COMMENT ON COLUMN property_owners.user_id IS
'소유자 사용자 식별자';

COMMENT ON COLUMN property_owners.ownership_ratio IS
'해당 사용자의 부동산 소유 지분율. 0 초과 100 이하의 비율로 관리한다';

COMMENT ON COLUMN property_owners.verified_at IS
'해당 소유관계의 검증 완료 일시';

COMMENT ON COLUMN property_owners.created_at IS
'소유관계 생성 일시';

COMMENT ON COLUMN property_owners.updated_at IS
'소유관계 최종 수정 일시';


-- ============================================================
-- 9. registry_snapshots Column Comments
-- ============================================================

COMMENT ON COLUMN registry_snapshots.id IS
'등기정보 스냅샷 식별자';

COMMENT ON COLUMN registry_snapshots.property_id IS
'등기정보 대상 부동산 식별자';

COMMENT ON COLUMN registry_snapshots.source IS
'등기정보 데이터 출처 코드. REGISTRY_SOURCE 코드 그룹을 사용한다';

COMMENT ON COLUMN registry_snapshots.observed_at IS
'해당 등기정보가 관찰 또는 수집된 기준 일시';

COMMENT ON COLUMN registry_snapshots.document_hash IS
'수집된 등기 문서 또는 원본 데이터의 무결성 검증용 해시값';

COMMENT ON COLUMN registry_snapshots.created_at IS
'등기정보 스냅샷 생성 일시';


-- ============================================================
-- 10. registry_rights Column Comments
-- ============================================================

COMMENT ON COLUMN registry_rights.id IS
'등기 권리 식별자';

COMMENT ON COLUMN registry_rights.registry_snapshot_id IS
'등기 권리가 속한 등기정보 스냅샷 식별자';

COMMENT ON COLUMN registry_rights.right_type IS
'등기 권리 유형 코드. REGISTRY_RIGHT_TYPE 코드 그룹을 사용한다';

COMMENT ON COLUMN registry_rights.holder_name IS
'해당 권리의 권리자 명칭';

COMMENT ON COLUMN registry_rights.amount IS
'해당 권리와 관련된 금액';

COMMENT ON COLUMN registry_rights.priority IS
'등기 권리의 우선순위. 값이 작을수록 우선순위가 높음을 의미한다';

COMMENT ON COLUMN registry_rights.registered_at IS
'해당 권리가 등기된 일시';

COMMENT ON COLUMN registry_rights.status IS
'등기 권리의 현재 유효 상태 코드. REGISTRY_RIGHT_STATUS 코드 그룹을 사용한다';

COMMENT ON COLUMN registry_rights.created_at IS
'등기 권리 데이터 생성 일시';


-- ============================================================
-- 11. listings Column Comments
-- ============================================================

COMMENT ON COLUMN listings.id IS
'매물 식별자';

COMMENT ON COLUMN listings.property_id IS
'매물 대상 부동산 식별자';

COMMENT ON COLUMN listings.seller_user_id IS
'매물을 등록한 판매자 또는 임대인 사용자 식별자';

COMMENT ON COLUMN listings.transaction_type IS
'매물 거래 유형 코드. LISTING_TRANSACTION 코드 그룹을 사용한다';

COMMENT ON COLUMN listings.deposit_amount IS
'임대차 거래의 보증금';

COMMENT ON COLUMN listings.monthly_rent IS
'월세 거래의 월 차임';

COMMENT ON COLUMN listings.sale_price IS
'매매 거래의 매매 가격';

COMMENT ON COLUMN listings.status IS
'매물 게시 및 거래 상태 코드. LISTING_STATUS 코드 그룹을 사용한다';

COMMENT ON COLUMN listings.listed_at IS
'매물 등록 일시';

COMMENT ON COLUMN listings.expired_at IS
'매물 게시 만료 일시';

COMMENT ON COLUMN listings.created_at IS
'매물 데이터 생성 일시';

COMMENT ON COLUMN listings.updated_at IS
'매물 데이터 최종 수정 일시';


-- ============================================================
-- 12. contracts Column Comments
-- ============================================================

COMMENT ON COLUMN contracts.id IS
'계약 식별자';

COMMENT ON COLUMN contracts.property_id IS
'계약 대상 부동산 식별자';

COMMENT ON COLUMN contracts.listing_id IS
'계약과 연결된 매물 식별자. 매물 없이 직접 생성된 계약은 NULL일 수 있다';

COMMENT ON COLUMN contracts.landlord_user_id IS
'계약상 임대인 사용자 식별자';

COMMENT ON COLUMN contracts.tenant_user_id IS
'계약상 임차인 사용자 식별자';

COMMENT ON COLUMN contracts.contract_type IS
'계약 유형 코드. CONTRACT_TYPE 코드 그룹을 사용한다';

COMMENT ON COLUMN contracts.deposit_amount IS
'계약 보증금';

COMMENT ON COLUMN contracts.monthly_rent IS
'계약 월 차임';

COMMENT ON COLUMN contracts.start_date IS
'계약 시작일';

COMMENT ON COLUMN contracts.end_date IS
'계약 종료일';

COMMENT ON COLUMN contracts.status IS
'계약 진행 상태 코드. CONTRACT_STATUS 코드 그룹을 사용한다';

COMMENT ON COLUMN contracts.signed_at IS
'계약 서명 또는 체결 완료 일시';

COMMENT ON COLUMN contracts.created_at IS
'계약 데이터 생성 일시';

COMMENT ON COLUMN contracts.updated_at IS
'계약 데이터 최종 수정 일시';


-- ============================================================
-- 13. contract_special_terms Column Comments
-- ============================================================

COMMENT ON COLUMN contract_special_terms.id IS
'계약 특약 식별자';

COMMENT ON COLUMN contract_special_terms.contract_id IS
'특약이 속한 계약 식별자';

COMMENT ON COLUMN contract_special_terms.term_type IS
'특약 유형 코드. CONTRACT_TERM_TYPE 코드 그룹을 사용한다';

COMMENT ON COLUMN contract_special_terms.content IS
'특약의 실제 문구 또는 상세 내용';

COMMENT ON COLUMN contract_special_terms.sequence IS
'계약서 내 특약 표시 및 처리 순서';

COMMENT ON COLUMN contract_special_terms.created_at IS
'특약 데이터 생성 일시';

COMMENT ON COLUMN contract_special_terms.updated_at IS
'특약 데이터 최종 수정 일시';


-- ============================================================
-- 14. risk_analyses Column Comments
-- ============================================================

COMMENT ON COLUMN risk_analyses.id IS
'위험 분석 결과 식별자';

COMMENT ON COLUMN risk_analyses.property_id IS
'위험 분석 대상 부동산 식별자';

COMMENT ON COLUMN risk_analyses.registry_snapshot_id IS
'분석 시 사용한 등기정보 스냅샷 식별자';

COMMENT ON COLUMN risk_analyses.analysis_version IS
'위험 분석 규칙 또는 분석 엔진의 버전';

COMMENT ON COLUMN risk_analyses.risk_level IS
'종합 위험 등급 코드. RISK_LEVEL 코드 그룹을 사용한다';

COMMENT ON COLUMN risk_analyses.risk_score IS
'종합 위험 점수. 0 이상 100 이하 범위로 관리한다';

COMMENT ON COLUMN risk_analyses.analyzed_at IS
'위험 분석이 수행된 일시';

COMMENT ON COLUMN risk_analyses.created_at IS
'위험 분석 결과 생성 일시';


-- ============================================================
-- 15. risk_factors Column Comments
-- ============================================================

COMMENT ON COLUMN risk_factors.id IS
'위험 요인 식별자';

COMMENT ON COLUMN risk_factors.risk_analysis_id IS
'해당 위험 요인이 속한 위험 분석 결과 식별자';

COMMENT ON COLUMN risk_factors.factor_type IS
'위험 요인 유형 코드. RISK_FACTOR_TYPE 코드 그룹을 사용한다';

COMMENT ON COLUMN risk_factors.severity IS
'개별 위험 요인의 심각도 코드. RISK_SEVERITY 코드 그룹을 사용한다';

COMMENT ON COLUMN risk_factors.value IS
'위험 요인과 관련된 원본 또는 정량 값';

COMMENT ON COLUMN risk_factors.description IS
'위험 요인의 상세 설명 및 판단 근거';

COMMENT ON COLUMN risk_factors.created_at IS
'위험 요인 생성 일시';