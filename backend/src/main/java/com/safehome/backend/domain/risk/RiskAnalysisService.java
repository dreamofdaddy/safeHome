package com.safehome.backend.domain.risk;

import com.safehome.backend.common.code.CodeService;
import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import com.safehome.backend.common.exception.PropertyNotFoundException;
import com.safehome.backend.common.exception.RegistrySnapshotNotFoundException;
import com.safehome.backend.common.exception.RiskAnalysisNotFoundException;
import com.safehome.backend.domain.property.PropertyRepository;
import com.safehome.backend.domain.risk.dto.RiskAnalysisCreateRequest;
import com.safehome.backend.domain.risk.dto.RiskAnalysisResponse;
import com.safehome.backend.domain.risk.dto.RiskAnalysisUpdateRequest;
import com.safehome.backend.domain.risk.dto.RiskFactorResponse;
import com.safehome.backend.domain.risk.dto.SafetyAnalysisResponse;
import com.safehome.backend.domain.registry.RegistrySnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RiskAnalysisService {

    private final CodeService codeService;
    private final RiskAnalysisRepository riskAnalysisRepository;
    private final RiskFactorRepository riskFactorRepository;
    private final PropertyRepository propertyRepository;
    private final RegistrySnapshotRepository registrySnapshotRepository;

    public RiskAnalysisService(
            RiskAnalysisRepository riskAnalysisRepository,
            RiskFactorRepository riskFactorRepository,
            PropertyRepository propertyRepository,
            RegistrySnapshotRepository registrySnapshotRepository,
            CodeService codeService
    ) {
        this.riskAnalysisRepository = riskAnalysisRepository;
        this.riskFactorRepository = riskFactorRepository;
        this.propertyRepository = propertyRepository;
        this.registrySnapshotRepository = registrySnapshotRepository;
        this.codeService = codeService;
    }

    public List<RiskAnalysisResponse> findAll() {
        return riskAnalysisRepository.findAll()
                .stream()
                .map(RiskAnalysisResponse::from)
                .toList();
    }

    public RiskAnalysisResponse findById(Long id) {
        RiskAnalysis riskAnalysis = riskAnalysisRepository.findById(id)
                .orElseThrow(() -> new RiskAnalysisNotFoundException(id));

        return RiskAnalysisResponse.from(riskAnalysis);
    }

    public SafetyAnalysisResponse findResultById(Long id) {
        RiskAnalysis analysis = riskAnalysisRepository.findById(id)
                .orElseThrow(() -> new RiskAnalysisNotFoundException(id));

        return new SafetyAnalysisResponse(
                analysis.getId(),
                analysis.getPropertyId(),
                analysis.getRegistrySnapshotId(),
                analysis.getAnalysisVersion(),
                analysis.getPolicyVersion(),
                analysis.getRiskLevel(),
                analysis.getRiskScore(),
                analysis.getAnalyzedAt(),
                riskFactorRepository.findByRiskAnalysisIdOrderByIdAsc(id)
                        .stream()
                        .map(RiskFactorResponse::from)
                        .toList()
        );
    }

    public List<RiskAnalysisResponse> findByPropertyId(Long propertyId) {
        propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException(propertyId));

        return riskAnalysisRepository
                .findByPropertyIdOrderByAnalyzedAtDesc(propertyId)
                .stream()
                .map(RiskAnalysisResponse::from)
                .toList();
    }

    public RiskAnalysisResponse findLatestByPropertyId(Long propertyId) {
        propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException(propertyId));

        RiskAnalysis riskAnalysis = riskAnalysisRepository
                .findTopByPropertyIdOrderByAnalyzedAtDesc(propertyId)
                .orElseThrow(() -> new RiskAnalysisNotFoundException(propertyId));

        return RiskAnalysisResponse.from(riskAnalysis);
    }

    @Transactional
    public RiskAnalysisResponse create(RiskAnalysisCreateRequest request) {
        validateProperty(request.getPropertyId());

        if (request.getRegistrySnapshotId() != null) {
            validateRegistrySnapshot(request.getRegistrySnapshotId());
        }

        validateRiskLevel(request.getRiskLevel());

        RiskAnalysis riskAnalysis = new RiskAnalysis(
                request.getPropertyId(),
                request.getRegistrySnapshotId(),
                request.getAnalysisVersion(),
                null,
                request.getRiskLevel(),
                request.getRiskScore(),
                request.getAnalyzedAt()
        );

        RiskAnalysis saved = riskAnalysisRepository.save(riskAnalysis);

        return RiskAnalysisResponse.from(saved);
    }

    @Transactional
    public RiskAnalysisResponse update(
            Long id,
            RiskAnalysisUpdateRequest request
    ) {
        RiskAnalysis riskAnalysis = riskAnalysisRepository.findById(id)
                .orElseThrow(() -> new RiskAnalysisNotFoundException(id));

        if (request.getRegistrySnapshotId() != null) {
            validateRegistrySnapshot(request.getRegistrySnapshotId());
        }

        validateRiskLevel(request.getRiskLevel());

        riskAnalysis.update(
                request.getRegistrySnapshotId(),
                request.getAnalysisVersion(),
                request.getRiskLevel(),
                request.getRiskScore(),
                request.getAnalyzedAt()
        );

        return RiskAnalysisResponse.from(riskAnalysis);
    }

    @Transactional
    public void delete(Long id) {
        RiskAnalysis riskAnalysis = riskAnalysisRepository.findById(id)
                .orElseThrow(() -> new RiskAnalysisNotFoundException(id));

        riskAnalysisRepository.delete(riskAnalysis);
    }

    private void validateProperty(Long propertyId) {
        propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException(propertyId));
    }

    private void validateRegistrySnapshot(Long registrySnapshotId) {
        registrySnapshotRepository.findById(registrySnapshotId)
                .orElseThrow(
                        () -> new RegistrySnapshotNotFoundException(registrySnapshotId)
                );
    }

    private void validateRiskLevel(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        codeService.getRequiredActiveCode(
                CommonCodes.RISK_LEVEL,
                riskLevel
        );
    }
}
