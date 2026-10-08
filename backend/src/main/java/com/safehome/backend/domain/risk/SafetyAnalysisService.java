package com.safehome.backend.domain.risk;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import com.safehome.backend.common.exception.PropertyNotFoundException;
import com.safehome.backend.common.exception.RegistrySnapshotNotFoundException;
import com.safehome.backend.domain.property.PropertyRepository;
import com.safehome.backend.domain.contract.Contract;
import com.safehome.backend.domain.contract.ContractRepository;
import com.safehome.backend.domain.listing.Listing;
import com.safehome.backend.domain.listing.ListingRepository;
import com.safehome.backend.domain.property.PropertyOwner;
import com.safehome.backend.domain.property.PropertyOwnerRepository;
import com.safehome.backend.domain.registry.RegistryRight;
import com.safehome.backend.domain.registry.RegistryRightRepository;
import com.safehome.backend.domain.registry.RegistrySnapshot;
import com.safehome.backend.domain.registry.RegistrySnapshotRepository;
import com.safehome.backend.domain.risk.analysis.RiskAnalysisContext;
import com.safehome.backend.domain.risk.analysis.RiskAnalysisEngine;
import com.safehome.backend.domain.risk.analysis.RiskLevelCalculator;
import com.safehome.backend.domain.risk.analysis.RiskRuleResult;
import com.safehome.backend.domain.risk.analysis.RiskScoreCalculationResult;
import com.safehome.backend.domain.risk.analysis.RiskScoreCalculator;
import com.safehome.backend.domain.risk.analysis.ScoredRiskFactor;
import com.safehome.backend.domain.risk.dto.RiskFactorResponse;
import com.safehome.backend.domain.risk.dto.SafetyAnalysisResponse;
import com.safehome.backend.domain.risk.policy.RiskPolicyVersionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SafetyAnalysisService {

    private static final String ANALYSIS_VERSION = "1.0";

    private final ContractRepository contractRepository;
    private final ListingRepository listingRepository;
    private final PropertyRepository propertyRepository;
    private final RegistrySnapshotRepository registrySnapshotRepository;
    private final RegistryRightRepository registryRightRepository;
    private final PropertyOwnerRepository propertyOwnerRepository;
    private final RiskAnalysisRepository riskAnalysisRepository;
    private final RiskFactorRepository riskFactorRepository;

    private final RiskPolicyVersionService riskPolicyVersionService;
    private final RiskAnalysisEngine riskAnalysisEngine;
    private final RiskScoreCalculator riskScoreCalculator;
    private final RiskLevelCalculator riskLevelCalculator;

    
    public SafetyAnalysisService(
            PropertyRepository propertyRepository,
            RegistrySnapshotRepository registrySnapshotRepository,
            RegistryRightRepository registryRightRepository,
            PropertyOwnerRepository propertyOwnerRepository,
            RiskAnalysisRepository riskAnalysisRepository,
            RiskFactorRepository riskFactorRepository,
            RiskPolicyVersionService riskPolicyVersionService,
            RiskAnalysisEngine riskAnalysisEngine,
            RiskScoreCalculator riskScoreCalculator,
            RiskLevelCalculator riskLevelCalculator,
            ListingRepository listingRepository,
            ContractRepository contractRepository
    ) {
        this.propertyRepository = propertyRepository;
        this.registrySnapshotRepository = registrySnapshotRepository;
        this.registryRightRepository = registryRightRepository;
        this.propertyOwnerRepository = propertyOwnerRepository;
        this.riskAnalysisRepository = riskAnalysisRepository;
        this.riskFactorRepository = riskFactorRepository;
        this.riskPolicyVersionService = riskPolicyVersionService;
        this.riskAnalysisEngine = riskAnalysisEngine;
        this.riskScoreCalculator = riskScoreCalculator;
        this.riskLevelCalculator = riskLevelCalculator;
        this.listingRepository = listingRepository;
        this.contractRepository = contractRepository;
    }

    @Transactional
    public SafetyAnalysisResponse analyze(Long propertyId) {

        validateProperty(propertyId);

        RegistrySnapshot snapshot =
                registrySnapshotRepository
                        .findTopByPropertyIdOrderByObservedAtDesc(propertyId)
                        .orElseThrow(() ->
                                new RegistrySnapshotNotFoundException(propertyId)
                        );

        List<RegistryRight> registryRights =
                registryRightRepository
                        .findByRegistrySnapshotIdAndStatus(
                                snapshot.getId(),
                                CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
                        );

        List<PropertyOwner> propertyOwners =
                propertyOwnerRepository.findByPropertyId(propertyId);

        Contract contract =
                contractRepository
                        .findTopByPropertyIdAndStatusOrderByStartDateDesc(
                                propertyId,
                                CommonCodes.CONTRACT_STATUS_ACTIVE
                        )
                        .orElse(null);

        Listing listing = null;

        if (contract != null && contract.getListingId() != null) {
            listing = listingRepository
                    .findById(contract.getListingId())
                    .orElse(null);
        }

        String policyVersion =
                riskPolicyVersionService.getActivePolicyVersion();

        RiskAnalysisContext context = new RiskAnalysisContext(
                propertyId,
                snapshot.getId(),
                registryRights,
                propertyOwners,
                listing,
                contract
        );

        List<RiskRuleResult> ruleResults =
                riskAnalysisEngine.analyze(context);

        RiskScoreCalculationResult scoreResult =
                riskScoreCalculator.calculate(ruleResults);

        String riskLevel =
                riskLevelCalculator.calculate(
                        scoreResult.getTotalScore()
                );

        LocalDateTime analyzedAt = LocalDateTime.now();

        RiskAnalysis riskAnalysis = new RiskAnalysis(
                propertyId,
                snapshot.getId(),
                ANALYSIS_VERSION,
                policyVersion,
                riskLevel,
                scoreResult.getTotalScore(),
                analyzedAt
        );

        RiskAnalysis savedAnalysis =
                riskAnalysisRepository.save(riskAnalysis);

        List<RiskFactor> riskFactors =
                scoreResult.getFactors()
                        .stream()
                        .map(factor -> toRiskFactor(savedAnalysis, factor))
                        .toList();

        List<RiskFactor> savedFactors =
                riskFactorRepository.saveAll(riskFactors);

        return new SafetyAnalysisResponse(
                savedAnalysis.getId(),
                savedAnalysis.getPropertyId(),
                savedAnalysis.getRegistrySnapshotId(),
                savedAnalysis.getAnalysisVersion(),
                savedAnalysis.getPolicyVersion(),
                savedAnalysis.getRiskLevel(),
                savedAnalysis.getRiskScore(),
                savedAnalysis.getAnalyzedAt(),
                savedFactors.stream()
                        .map(RiskFactorResponse::from)
                        .toList()
        );
    }

    private RiskFactor toRiskFactor(
            RiskAnalysis riskAnalysis,
            ScoredRiskFactor factor
    ) {
        return new RiskFactor(
                riskAnalysis.getId(),
                factor.getFactorType(),
                factor.getSeverity(),
                factor.getScore(),
                factor.getValue(),
                factor.getDescription()
        );
    }

    private void validateProperty(Long propertyId) {
        if (propertyId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        propertyRepository.findById(propertyId)
                .orElseThrow(() ->
                        new PropertyNotFoundException(propertyId)
                );
    }
}