package com.safehome.backend.domain.property;

import com.safehome.backend.common.code.CodeService;
import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.common.exception.PropertyNotFoundException;
import com.safehome.backend.domain.contract.ContractRepository;
import com.safehome.backend.domain.contract.dto.ContractResponse;
import com.safehome.backend.domain.listing.ListingRepository;
import com.safehome.backend.domain.listing.dto.ListingResponse;
import com.safehome.backend.domain.property.dto.PropertyOwnerResponse;
import com.safehome.backend.domain.property.dto.PropertyResponse;
import com.safehome.backend.domain.property.dto.PropertySafetySummaryResponse;
import com.safehome.backend.domain.registry.RegistryRightRepository;
import com.safehome.backend.domain.registry.RegistrySnapshot;
import com.safehome.backend.domain.registry.RegistrySnapshotRepository;
import com.safehome.backend.domain.registry.dto.RegistryRightResponse;
import com.safehome.backend.domain.registry.dto.RegistrySnapshotResponse;
import com.safehome.backend.domain.risk.RiskAnalysis;
import com.safehome.backend.domain.risk.RiskAnalysisRepository;
import com.safehome.backend.domain.risk.RiskFactorRepository;
import com.safehome.backend.domain.risk.dto.RiskAnalysisResponse;
import com.safehome.backend.domain.risk.dto.RiskFactorResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class PropertySafetySummaryService {

    private static final Map<String, Integer> SEVERITY_ORDER = Map.of(
            "LOW", 1,
            "MEDIUM", 2,
            "HIGH", 3,
            "CRITICAL", 4
    );

    private final CodeService codeService;
    private final PropertyRepository propertyRepository;
    private final PropertyOwnerRepository propertyOwnerRepository;
    private final RegistrySnapshotRepository registrySnapshotRepository;
    private final RegistryRightRepository registryRightRepository;
    private final RiskAnalysisRepository riskAnalysisRepository;
    private final RiskFactorRepository riskFactorRepository;
    private final ListingRepository listingRepository;
    private final ContractRepository contractRepository;

    public PropertySafetySummaryService(
            PropertyRepository propertyRepository,
            PropertyOwnerRepository propertyOwnerRepository,
            RegistrySnapshotRepository registrySnapshotRepository,
            RegistryRightRepository registryRightRepository,
            RiskAnalysisRepository riskAnalysisRepository,
            RiskFactorRepository riskFactorRepository,
            ListingRepository listingRepository,
            ContractRepository contractRepository,
            CodeService codeService
    ) {
        this.propertyRepository = propertyRepository;
        this.propertyOwnerRepository = propertyOwnerRepository;
        this.registrySnapshotRepository = registrySnapshotRepository;
        this.registryRightRepository = registryRightRepository;
        this.riskAnalysisRepository = riskAnalysisRepository;
        this.riskFactorRepository = riskFactorRepository;
        this.listingRepository = listingRepository;
        this.contractRepository = contractRepository;
        this.codeService = codeService;
    }

    public PropertySafetySummaryResponse getSafetySummary(Long propertyId) {

        // 1. Property 존재 여부 확인
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException(propertyId));

        String activeListingStatus =
                codeService.getRequiredActiveCode(
                        CommonCodes.LISTING_STATUS,
                        CommonCodes.LISTING_STATUS_ACTIVE
                );

        // 2. 소유자
        List<PropertyOwnerResponse> owners =
        propertyOwnerRepository.findByPropertyId(propertyId)
                .stream()
                .map(PropertyOwnerResponse::from)
                .toList();

        // 3. 최신 등기부 스냅샷
        RegistrySnapshot latestSnapshot = registrySnapshotRepository
                .findTopByPropertyIdOrderByObservedAtDesc(propertyId)
                .orElse(null);

        RegistrySnapshotResponse latestRegistrySnapshot =
                latestSnapshot == null
                        ? null
                        : RegistrySnapshotResponse.from(latestSnapshot);

        // 4. 최신 등기부의 등기 권리
        List<RegistryRightResponse> registryRights =
                latestSnapshot == null
                        ? List.of()
                        : registryRightRepository
                                .findByRegistrySnapshotId(latestSnapshot.getId())
                                .stream()
                                .map(RegistryRightResponse::from)
                                .toList();

        // 5. 최신 Risk Analysis
        RiskAnalysis latestRiskAnalysisEntity = riskAnalysisRepository
                .findTopByPropertyIdOrderByAnalyzedAtDesc(propertyId)
                .orElse(null);

        RiskAnalysisResponse latestRiskAnalysis =
                latestRiskAnalysisEntity == null
                        ? null
                        : RiskAnalysisResponse.from(latestRiskAnalysisEntity);

        // 6. 최신 Risk Analysis의 Risk Factors
        List<RiskFactorResponse> riskFactors =
                latestRiskAnalysisEntity == null
                        ? List.of()
                        : riskFactorRepository
                        .findByRiskAnalysisIdOrderByIdAsc(
                                latestRiskAnalysisEntity.getId())
                        .stream()
                        .map(RiskFactorResponse::from)
                        .toList();

        // 7. 현재 ACTIVE 매물
        List<ListingResponse> activeListings = listingRepository
                .findByPropertyIdAndStatusOrderByListedAtDesc(
                        propertyId,
                        activeListingStatus
                )
                .stream()
                .map(ListingResponse::from)
                .toList();

        // 8. 해당 부동산의 계약
        List<ContractResponse> contracts = contractRepository
                .findByPropertyIdOrderByStartDateDesc(propertyId)
                .stream()
                .map(ContractResponse::from)
                .toList();

        // 9. Safety Summary
        String summary = createSummary(
                latestRiskAnalysis,
                riskFactors.size()
        );

        String highestSeverity = findHighestSeverity(riskFactors);

        return new PropertySafetySummaryResponse(
                PropertyResponse.from(property),
                owners,
                latestRegistrySnapshot,
                registryRights,
                latestRiskAnalysis,
                riskFactors,
                activeListings,
                contracts,
                summary,
                highestSeverity,
                riskFactors.size()
        );
    }

    private String createSummary(
            RiskAnalysisResponse riskAnalysis,
            int factorCount
    ) {
        if (riskAnalysis == null) {
            return "아직 안전성 분석 결과가 없습니다.";
        }

        return switch (riskAnalysis.riskLevel()) {
            case "LOW" ->
                    "현재 분석에서 주요 위험 요소가 확인되지 않았습니다.";

            case "MEDIUM" ->
                    "주의가 필요한 위험 요소가 "
                            + factorCount
                            + "건 확인되었습니다.";

            case "HIGH" ->
                    "위험 요소가 "
                            + factorCount
                            + "건 확인되어 계약 전 상세 확인이 필요합니다.";

            case "CRITICAL" ->
                    "중대한 위험 요소가 "
                            + factorCount
                            + "건 확인되어 계약 전 추가 확인이 필요합니다.";

            default ->
                    "분석 결과를 확인해 주세요.";
        };
    }

    private String findHighestSeverity(
            List<RiskFactorResponse> riskFactors
    ) {
        return riskFactors.stream()
                .map(RiskFactorResponse::severity)
                .filter(SEVERITY_ORDER::containsKey)
                .max(Comparator.comparingInt(SEVERITY_ORDER::get))
                .orElse(null);
    }
}
