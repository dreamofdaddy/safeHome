-- =========================================================
-- SafeHome Risk Analysis Policy Result
-- =========================================================

ALTER TABLE risk_analyses
ADD COLUMN policy_version VARCHAR(50);

ALTER TABLE risk_analyses
ADD CONSTRAINT fk_risk_analyses_policy_version
FOREIGN KEY (policy_version)
REFERENCES risk_policy_versions (policy_version);


ALTER TABLE risk_factors
ADD COLUMN score NUMERIC(10, 2);

ALTER TABLE risk_factors
ADD CONSTRAINT ck_risk_factors_score
CHECK (score IS NULL OR score >= 0);


-- 활성 정책 버전은 하나만 허용
CREATE UNIQUE INDEX uk_risk_policy_versions_active
ON risk_policy_versions (use_yn)
WHERE use_yn = 'Y';