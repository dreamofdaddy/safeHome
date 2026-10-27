package com.safehome.backend.domain.risk.reanalysis;

public enum SafetyReanalysisReason {

    NO_PREVIOUS_ANALYSIS,
    POLICY_VERSION_CHANGED,
    ANALYSIS_VERSION_CHANGED,
    LISTING_CHANGED,
    REGISTRY_CHANGED,
    UP_TO_DATE
}