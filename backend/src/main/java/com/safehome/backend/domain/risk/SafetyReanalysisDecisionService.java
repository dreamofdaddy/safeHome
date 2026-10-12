package com.safehome.backend.domain.risk;

import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import com.safehome.backend.common.exception.PropertyNotFoundException;
import com.safehome.backend.common.exception.RegistrySnapshotNotFoundException;
import com.safehome.backend.domain.property.PropertyRepository;
import com.safehome.backend.domain.registry.RegistrySnapshot;
import com.safehome.backend.domain.registry.RegistrySnapshotComparisonService;
import com.safehome.backend.domain.registry.RegistrySnapshotRepository;
import com.safehome.backend.domain.registry.dto.RegistrySnapshotComparisonResponse;
import com.safehome.backend.domain.risk.dto.SafetyReanalysisDecisionResponse;
import com.safehome.backend.domain.risk.policy.RiskPolicyVersionService;
import com.safehome.backend.domain.risk.reanalysis.SafetyReanalysisReason;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class SafetyReanalysisDecisionService {

    private final PropertyRepository propertyRepository;
    private final RegistrySnapshotRepository registrySnapshotRepository;
    private final RiskAnalysisRepository riskAnalysisRepository;
    private final RiskPolicyVersionService riskPolicyVersionService;
    private final RegistrySnapshotComparisonService
            registrySnapshotComparisonService;

    public SafetyReanalysisDecisionService(
            PropertyRepository propertyRepository,
            RegistrySnapshotRepository registrySnapshotRepository,
            RiskAnalysisRepository riskAnalysisRepository,
            RiskPolicyVersionService riskPolicyVersionService,
            RegistrySnapshotComparisonService
                    registrySnapshotComparisonService
    ) {
        this.propertyRepository = propertyRepository;
        this.registrySnapshotRepository = registrySnapshotRepository;
        this.riskAnalysisRepository = riskAnalysisRepository;
        this.riskPolicyVersionService = riskPolicyVersionService;
        this.registrySnapshotComparisonService =
                registrySnapshotComparisonService;
    }

    public SafetyReanalysisDecisionResponse decide(
            Long propertyId
    ) {
        validateProperty(propertyId);

        RegistrySnapshot latestSnapshot =
                registrySnapshotRepository
                        .findTopByPropertyIdOrderByObservedAtDescIdDesc(
                                propertyId
                        )
                        .orElseThrow(() ->
                                new RegistrySnapshotNotFoundException(
                                        propertyId
                                )
                        );

        String activePolicyVersion =
                riskPolicyVersionService
                        .getActivePolicyVersion();

        RiskAnalysis latestAnalysis =
                riskAnalysisRepository
                        .findTopByPropertyIdOrderByAnalyzedAtDescIdDesc(
                                propertyId
                        )
                        .orElse(null);

        if (latestAnalysis == null) {
            return required(
                    propertyId,
                    SafetyReanalysisReason.NO_PREVIOUS_ANALYSIS,
                    latestSnapshot.getId(),
                    null,
                    activePolicyVersion,
                    null
            );
        }

        if (!Objects.equals(
                latestAnalysis.getPolicyVersion(),
                activePolicyVersion
        )) {
            return required(
                    propertyId,
                    SafetyReanalysisReason.POLICY_VERSION_CHANGED,
                    latestSnapshot.getId(),
                    latestAnalysis.getRegistrySnapshotId(),
                    activePolicyVersion,
                    latestAnalysis.getPolicyVersion()
            );
        }

        Long lastAnalyzedSnapshotId =
                latestAnalysis.getRegistrySnapshotId();

        /*
         * 과거 수동 생성 RiskAnalysis처럼 snapshot 추적 정보가
         * 없는 데이터는 최신 상태를 분석했다고 보장할 수 없다.
         */
        if (lastAnalyzedSnapshotId == null) {
            return required(
                    propertyId,
                    SafetyReanalysisReason.REGISTRY_CHANGED,
                    latestSnapshot.getId(),
                    null,
                    activePolicyVersion,
                    latestAnalysis.getPolicyVersion()
            );
        }

        if (Objects.equals(
                lastAnalyzedSnapshotId,
                latestSnapshot.getId()
        )) {
            return notRequired(
                    propertyId,
                    latestSnapshot.getId(),
                    lastAnalyzedSnapshotId,
                    activePolicyVersion,
                    latestAnalysis.getPolicyVersion()
            );
        }

        RegistrySnapshotComparisonResponse comparison =
                registrySnapshotComparisonService.compare(
                        propertyId,
                        lastAnalyzedSnapshotId,
                        latestSnapshot.getId()
                );

        if (comparison.rightsChanged()) {
            return required(
                    propertyId,
                    SafetyReanalysisReason.REGISTRY_CHANGED,
                    latestSnapshot.getId(),
                    lastAnalyzedSnapshotId,
                    activePolicyVersion,
                    latestAnalysis.getPolicyVersion()
            );
        }

        return notRequired(
                propertyId,
                latestSnapshot.getId(),
                lastAnalyzedSnapshotId,
                activePolicyVersion,
                latestAnalysis.getPolicyVersion()
        );
    }

    private SafetyReanalysisDecisionResponse required(
            Long propertyId,
            SafetyReanalysisReason reason,
            Long latestSnapshotId,
            Long lastAnalyzedSnapshotId,
            String activePolicyVersion,
            String lastPolicyVersion
    ) {
        return new SafetyReanalysisDecisionResponse(
                propertyId,
                true,
                reason,
                latestSnapshotId,
                lastAnalyzedSnapshotId,
                activePolicyVersion,
                lastPolicyVersion
        );
    }

    private SafetyReanalysisDecisionResponse notRequired(
            Long propertyId,
            Long latestSnapshotId,
            Long lastAnalyzedSnapshotId,
            String activePolicyVersion,
            String lastPolicyVersion
    ) {
        return new SafetyReanalysisDecisionResponse(
                propertyId,
                false,
                SafetyReanalysisReason.UP_TO_DATE,
                latestSnapshotId,
                lastAnalyzedSnapshotId,
                activePolicyVersion,
                lastPolicyVersion
        );
    }

    private void validateProperty(Long propertyId) {
        if (propertyId == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST
            );
        }

        propertyRepository.findById(propertyId)
                .orElseThrow(() ->
                        new PropertyNotFoundException(propertyId)
                );
    }
}