-- ============================================================
-- SafeHome
-- V6: Add Risk Policy V2
--
-- 목적:
-- 1. 기존 V1 정책은 이력 보존
-- 2. V2 정책을 신규 생성
-- 3. 신규 Risk Factor 정책 추가
--    - CO_OWNER_ISSUE
--    - LISTING_CONTRACT_MISMATCH
-- 4. V1 -> V2 Risk Level 정책 복제
-- 5. V2를 유일한 Active Policy로 설정
-- ============================================================

-- ------------------------------------------------------------
-- 1. 기존 V1 비활성화
--    partial unique index(활성 정책 1개)를 만족하기 위해
--    V2 활성화 전에 V1을 먼저 비활성화한다.
-- ------------------------------------------------------------
UPDATE risk_policy_versions
SET
    use_yn = 'N',
    updated_at = CURRENT_TIMESTAMP
WHERE policy_version = 'V1'
  AND use_yn = 'Y';


-- ------------------------------------------------------------
-- 2. V2 정책 버전 생성
-- ------------------------------------------------------------
INSERT INTO risk_policy_versions (
    policy_version,
    policy_name,
    description,
    use_yn,
    created_at,
    updated_at
)
VALUES (
    'V2',
    'SafeHome Risk Policy V2',
    'SafeHome 부동산 안전성 분석 정책 V2',
    'Y',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);


-- ------------------------------------------------------------
-- 3. 기존 Risk Factor 정책 복사
-- ------------------------------------------------------------
INSERT INTO risk_factor_policies (
    policy_version,
    factor_type,
    severity,
    score,
    description,
    use_yn,
    created_at,
    updated_at
)
SELECT
    'V2',
    factor_type,
    severity,
    score,
    description,
    use_yn,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM risk_factor_policies
WHERE policy_version = 'V1';


-- ------------------------------------------------------------
-- 4. V2 신규 Risk Factor 정책 추가
-- ------------------------------------------------------------

INSERT INTO risk_factor_policies (
    policy_version,
    factor_type,
    severity,
    score,
    description,
    use_yn,
    created_at,
    updated_at
)
VALUES
(
    'V2',
    'CO_OWNER_ISSUE',
    'HIGH',
    25.00,
    '공동소유자 중 계약 참여 또는 동의 여부를 추가 확인해야 합니다.',
    'Y',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'V2',
    'LISTING_CONTRACT_MISMATCH',
    'HIGH',
    20.00,
    '계약과 연결된 매물 정보가 일치하지 않습니다.',
    'Y',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);


-- ------------------------------------------------------------
-- 5. V1 Risk Level 정책을 V2로 복사
-- ------------------------------------------------------------
INSERT INTO risk_level_policies (
    policy_version,
    risk_level,
    min_score,
    max_score,
    sort_order,
    use_yn,
    created_at,
    updated_at
)
SELECT
    'V2',
    risk_level,
    min_score,
    max_score,
    sort_order,
    use_yn,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM risk_level_policies
WHERE policy_version = 'V1';