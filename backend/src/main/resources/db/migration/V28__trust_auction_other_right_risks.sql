ALTER TABLE registry_rights ADD COLUMN source_name VARCHAR(200);
COMMENT ON COLUMN registry_rights.source_name IS '문서에 표시된 짧은 원문 권리명. 이전 자료는 null 유지';
INSERT INTO code_values(group_id,code,code_name,description,sort_order,use_yn,created_at,updated_at)
SELECT g.id,v.code,v.name,v.description,v.sort_order,'Y',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM code_groups g CROSS JOIN (VALUES
 ('TRUST','신탁','유효한 신탁 등기',8),('AUCTION','경매','유효한 경매 관련 등기',9)
) AS v(code,name,description,sort_order)
WHERE g.group_code IN ('REGISTRY_RIGHT_TYPE','RISK_FACTOR_TYPE');
-- Add new combinations only; do not alter existing policies or saved analyses.
INSERT INTO risk_factor_policies(policy_version,factor_type,severity,score,description,use_yn,created_at,updated_at)
SELECT p.policy_version,v.factor_type,v.severity,v.score,v.description,'Y',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM risk_policy_versions p CROSS JOIN (VALUES
 ('TRUST','CRITICAL',70,'유효한 신탁 등기'),
 ('AUCTION','CRITICAL',70,'유효한 경매 관련 등기'),
 ('OTHER','HIGH',40,'유효한 기타 등기 권리: 원문 권리명 및 내용 확인 필요')
) AS v(factor_type,severity,score,description);
