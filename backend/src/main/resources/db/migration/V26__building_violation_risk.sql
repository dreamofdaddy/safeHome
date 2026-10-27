ALTER TABLE risk_analyses ADD COLUMN building_register_input_fingerprint VARCHAR(120);
INSERT INTO code_values(group_id,code,code_name,description,sort_order,use_yn,created_at,updated_at)
SELECT id,'BUILDING_VIOLATION','건축물대장 위반 표시','첨부 건축물대장의 위반건축물 표시',7,'Y',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM code_groups WHERE group_code='RISK_FACTOR_TYPE';
-- Add only the newly introduced factor; existing factor scores and saved analyses are unchanged.
INSERT INTO risk_factor_policies(policy_version,factor_type,severity,score,description,use_yn,created_at,updated_at)
SELECT policy_version,'BUILDING_VIOLATION','HIGH',50,'첨부 건축물대장에 위반건축물 표시 확인','Y',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM risk_policy_versions;
