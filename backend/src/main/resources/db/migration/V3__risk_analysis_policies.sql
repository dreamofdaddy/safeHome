-- =========================================================
-- SafeHome Risk Analysis Policy
-- =========================================================

CREATE TABLE risk_policy_versions (
    id              BIGSERIAL PRIMARY KEY,
    policy_version  VARCHAR(50) NOT NULL UNIQUE,
    policy_name     VARCHAR(100) NOT NULL,
    description     TEXT,
    use_yn          CHAR(1) NOT NULL DEFAULT 'Y',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_risk_policy_versions_use_yn
        CHECK (use_yn IN ('Y', 'N'))
);

COMMENT ON TABLE risk_policy_versions IS
    'Risk 분석 정책 버전';

COMMENT ON COLUMN risk_policy_versions.policy_version IS
    'Risk 분석 정책 버전 식별자';

COMMENT ON COLUMN risk_policy_versions.policy_name IS
    '정책명';

COMMENT ON COLUMN risk_policy_versions.use_yn IS
    '정책 버전 사용 여부';


CREATE TABLE risk_factor_policies (
    id              BIGSERIAL PRIMARY KEY,
    policy_version  VARCHAR(50) NOT NULL,
    factor_type     VARCHAR(50) NOT NULL,
    severity        VARCHAR(50) NOT NULL,
    score           NUMERIC(10, 2) NOT NULL DEFAULT 0,
    description     TEXT,
    use_yn          CHAR(1) NOT NULL DEFAULT 'Y',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_risk_factor_policy
        UNIQUE (policy_version, factor_type, severity),

    CONSTRAINT fk_risk_factor_policy_version
        FOREIGN KEY (policy_version)
        REFERENCES risk_policy_versions (policy_version),

    CONSTRAINT ck_risk_factor_policy_score
        CHECK (score >= 0),

    CONSTRAINT ck_risk_factor_policy_use_yn
        CHECK (use_yn IN ('Y', 'N'))
);

COMMENT ON TABLE risk_factor_policies IS
    'Risk Factor별 점수 정책';

COMMENT ON COLUMN risk_factor_policies.policy_version IS
    '적용 정책 버전';

COMMENT ON COLUMN risk_factor_policies.factor_type IS
    'Risk Factor 유형';

COMMENT ON COLUMN risk_factor_policies.severity IS
    'Risk Severity';

COMMENT ON COLUMN risk_factor_policies.score IS
    '해당 위험 요소의 점수';


CREATE TABLE risk_level_policies (
    id              BIGSERIAL PRIMARY KEY,
    policy_version  VARCHAR(50) NOT NULL,
    risk_level      VARCHAR(50) NOT NULL,
    min_score       NUMERIC(10, 2) NOT NULL,
    max_score       NUMERIC(10, 2),
    sort_order      INT NOT NULL DEFAULT 0,
    use_yn          CHAR(1) NOT NULL DEFAULT 'Y',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_risk_level_policy
        UNIQUE (policy_version, risk_level),

    CONSTRAINT fk_risk_level_policy_version
        FOREIGN KEY (policy_version)
        REFERENCES risk_policy_versions (policy_version),

    CONSTRAINT ck_risk_level_policy_score_range
        CHECK (
            max_score IS NULL
            OR max_score >= min_score
        ),

    CONSTRAINT ck_risk_level_policy_use_yn
        CHECK (use_yn IN ('Y', 'N'))
);

COMMENT ON TABLE risk_level_policies IS
    'Risk 총점에 따른 위험 등급 정책';

COMMENT ON COLUMN risk_level_policies.policy_version IS
    '적용 정책 버전';

COMMENT ON COLUMN risk_level_policies.risk_level IS
    'Risk Level';

COMMENT ON COLUMN risk_level_policies.min_score IS
    '등급 최소 점수';

COMMENT ON COLUMN risk_level_policies.max_score IS
    '등급 최대 점수';


-- =========================================================
-- V1 Policy
-- =========================================================

INSERT INTO risk_policy_versions (
    policy_version,
    policy_name,
    description,
    use_yn
) VALUES (
    'V1',
    'SafeHome Risk Policy V1',
    '개발용 초기 리스크 분석 정책',
    'Y'
);


-- =========================================================
-- V1 Factor Score
-- =========================================================

INSERT INTO risk_factor_policies (
    policy_version,
    factor_type,
    severity,
    score,
    description,
    use_yn
) VALUES
(
    'V1',
    'MORTGAGE',
    'HIGH',
    30,
    '개발용 초기 정책: 활성 근저당권',
    'Y'
),
(
    'V1',
    'OWNERSHIP_MISMATCH',
    'HIGH',
    25,
    '개발용 초기 정책: 소유권 불일치',
    'Y'
),
(
    'V1',
    'PRIORITY_RIGHT',
    'HIGH',
    20,
    '개발용 초기 정책: 우선순위 권리',
    'Y'
),
(
    'V1',
    'SEIZURE',
    'CRITICAL',
    50,
    '개발용 초기 정책: 압류',
    'Y'
),
(
    'V1',
    'OTHER',
    'LOW',
    5,
    '개발용 초기 정책: 기타 위험 요소',
    'Y'
);


-- =========================================================
-- V1 Level Threshold
-- =========================================================

INSERT INTO risk_level_policies (
    policy_version,
    risk_level,
    min_score,
    max_score,
    sort_order,
    use_yn
) VALUES
(
    'V1',
    'LOW',
    0,
    19,
    1,
    'Y'
),
(
    'V1',
    'MEDIUM',
    20,
    39,
    2,
    'Y'
),
(
    'V1',
    'HIGH',
    40,
    69,
    3,
    'Y'
),
(
    'V1',
    'CRITICAL',
    70,
    NULL,
    4,
    'Y'
);