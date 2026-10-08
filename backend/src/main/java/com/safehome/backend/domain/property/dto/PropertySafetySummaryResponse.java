package com.safehome.backend.domain.property.dto;

import com.safehome.backend.domain.contract.dto.ContractResponse;
import com.safehome.backend.domain.listing.dto.ListingResponse;
import com.safehome.backend.domain.registry.dto.RegistryRightResponse;
import com.safehome.backend.domain.registry.dto.RegistrySnapshotResponse;
import com.safehome.backend.domain.risk.dto.RiskAnalysisResponse;
import com.safehome.backend.domain.risk.dto.RiskFactorResponse;

import java.util.List;

public record PropertySafetySummaryResponse(
        PropertyResponse property,
        List<PropertyOwnerResponse> owners,
        RegistrySnapshotResponse latestRegistrySnapshot,
        List<RegistryRightResponse> registryRights,
        RiskAnalysisResponse latestRiskAnalysis,
        List<RiskFactorResponse> riskFactors,
        List<ListingResponse> activeListings,
        List<ContractResponse> contracts,

        // Safety Summary
        String summary,
        String highestSeverity,
        int riskFactorCount
) {
    public PropertySafetySummaryResponse {
        owners = List.copyOf(owners);
        registryRights = List.copyOf(registryRights);
        riskFactors = List.copyOf(riskFactors);
        activeListings = List.copyOf(activeListings);
        contracts = List.copyOf(contracts);
    }
}
