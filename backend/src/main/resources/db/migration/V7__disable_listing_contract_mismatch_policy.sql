-- ============================================================
-- SafeHome
-- V7: Disable obsolete listing-contract mismatch risk policy
-- ============================================================

UPDATE risk_factor_policies
SET
    use_yn = 'N',
    updated_at = CURRENT_TIMESTAMP
WHERE policy_version = 'V2'
  AND factor_type = 'LISTING_CONTRACT_MISMATCH'
  AND use_yn = 'Y';