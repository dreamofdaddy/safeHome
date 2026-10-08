package com.safehome.backend.domain.risk;

import com.safehome.backend.common.code.CodeService;
import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import com.safehome.backend.common.exception.RiskAnalysisNotFoundException;
import com.safehome.backend.common.exception.RiskFactorNotFoundException;
import com.safehome.backend.domain.risk.dto.RiskFactorCreateRequest;
import com.safehome.backend.domain.risk.dto.RiskFactorResponse;
import com.safehome.backend.domain.risk.dto.RiskFactorUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RiskFactorService {

    private final CodeService codeService;
    private final RiskFactorRepository riskFactorRepository;
    private final RiskAnalysisRepository riskAnalysisRepository;

    public RiskFactorService(
            RiskFactorRepository riskFactorRepository,
            RiskAnalysisRepository riskAnalysisRepository,
            CodeService codeService
    ) {
        this.riskFactorRepository = riskFactorRepository;
        this.riskAnalysisRepository = riskAnalysisRepository;
        this.codeService = codeService;
    }

    public List<RiskFactorResponse> findAll() {
        return riskFactorRepository.findAll()
                .stream()
                .map(RiskFactorResponse::from)
                .toList();
    }

    public RiskFactorResponse findById(Long id) {
        RiskFactor riskFactor = riskFactorRepository.findById(id)
                .orElseThrow(() -> new RiskFactorNotFoundException(id));

        return RiskFactorResponse.from(riskFactor);
    }

    public List<RiskFactorResponse> findByRiskAnalysisId(Long riskAnalysisId) {
        validateRiskAnalysis(riskAnalysisId);

        return riskFactorRepository
                .findByRiskAnalysisIdOrderByIdAsc(riskAnalysisId)
                .stream()
                .map(RiskFactorResponse::from)
                .toList();
    }

    @Transactional
    public RiskFactorResponse create(RiskFactorCreateRequest request) {
        validateRiskAnalysis(request.getRiskAnalysisId());

        validateFactorType(request.getFactorType());
        validateSeverity(request.getSeverity());

        RiskFactor riskFactor = new RiskFactor(
                request.getRiskAnalysisId(),
                request.getFactorType(),
                request.getSeverity(),
                null,
                request.getValue(),
                request.getDescription()
        );

        RiskFactor saved = riskFactorRepository.save(riskFactor);

        return RiskFactorResponse.from(saved);
    }

    @Transactional
    public RiskFactorResponse update(
            Long id,
            RiskFactorUpdateRequest request
    ) {
        RiskFactor riskFactor = riskFactorRepository.findById(id)
                .orElseThrow(() -> new RiskFactorNotFoundException(id));

        validateFactorType(request.getFactorType());
        validateSeverity(request.getSeverity());

        riskFactor.update(
                request.getFactorType(),
                request.getSeverity(),
                request.getValue(),
                request.getDescription()
        );

        return RiskFactorResponse.from(riskFactor);
    }

    @Transactional
    public void delete(Long id) {
        RiskFactor riskFactor = riskFactorRepository.findById(id)
                .orElseThrow(() -> new RiskFactorNotFoundException(id));

        riskFactorRepository.delete(riskFactor);
    }

    private void validateRiskAnalysis(Long riskAnalysisId) {
        riskAnalysisRepository.findById(riskAnalysisId)
                .orElseThrow(
                        () -> new RiskAnalysisNotFoundException(riskAnalysisId)
                );
    }

    private void validateFactorType(String factorType) {
        if (factorType == null || factorType.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        codeService.getRequiredActiveCode(
                "RISK_FACTOR_TYPE",
                factorType
        );
    }

    private void validateSeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        codeService.getRequiredActiveCode(
                "RISK_SEVERITY",
                severity
        );
    }
}