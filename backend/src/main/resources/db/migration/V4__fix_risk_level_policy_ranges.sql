-- =========================================================
-- SafeHome Risk Level Policy Range Fix
-- Risk score scale: 2 decimal places
-- =========================================================

UPDATE risk_level_policies
SET max_score = 19.99,
    updated_at = CURRENT_TIMESTAMP
WHERE policy_version = 'V1'
  AND risk_level = 'LOW';

UPDATE risk_level_policies
SET max_score = 39.99,
    updated_at = CURRENT_TIMESTAMP
WHERE policy_version = 'V1'
  AND risk_level = 'MEDIUM';

UPDATE risk_level_policies
SET max_score = 69.99,
    updated_at = CURRENT_TIMESTAMP
WHERE policy_version = 'V1'
  AND risk_level = 'HIGH';