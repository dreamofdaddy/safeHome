-- ============================================================
-- SafeHome
-- V8: Add CO_OWNER_ISSUE risk factor common code
-- ============================================================

INSERT INTO code_values (
    group_id,
    code,
    code_name,
    description,
    sort_order,
    use_yn,
    created_at,
    updated_at
)
SELECT
    cg.id,
    'CO_OWNER_ISSUE',
    '공동소유자 이슈',
    '공동소유 부동산의 계약 진행 과정에서 공동소유자 관련 추가 확인이 필요한 경우',
    6,
    'Y',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM code_groups cg
WHERE cg.group_code = 'RISK_FACTOR_TYPE'
  AND NOT EXISTS (
      SELECT 1
      FROM code_values cv
      WHERE cv.group_id = cg.id
        AND cv.code = 'CO_OWNER_ISSUE'
  );