package com.safehome.backend.domain.risk;

import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import com.safehome.backend.common.exception.PropertyNotFoundException;
import com.safehome.backend.domain.property.PropertyRepository;
import com.safehome.backend.domain.risk.comparison.RiskFactorComparator;
import com.safehome.backend.domain.risk.comparison.RiskTrend;
import com.safehome.backend.domain.risk.comparison.RiskTrendCalculator;
import com.safehome.backend.domain.risk.dto.RiskAnalysisComparisonResponse;
import com.safehome.backend.domain.risk.dto.RiskAnalysisResponse;
import com.safehome.backend.domain.risk.dto.RiskFactorComparisonResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class RiskAnalysisComparisonService {

    private final PropertyRepository propertyRepository;
    private final RiskAnalysisRepository riskAnalysisRepository;
    private final RiskFactorRepository riskFactorRepository;
    private final RiskFactorComparator riskFactorComparator;
    private final RiskTrendCalculator riskTrendCalculator;

    public RiskAnalysisComparisonService(
            PropertyRepository propertyRepository,
            RiskAnalysisRepository riskAnalysisRepository,
            RiskFactorRepository riskFactorRepository,
            RiskFactorComparator riskFactorComparator,
            RiskTrendCalculator riskTrendCalculator
    ) {
        this.propertyRepository = propertyRepository;
        this.riskAnalysisRepository = riskAnalysisRepository;
        this.riskFactorRepository = riskFactorRepository;
        this.riskFactorComparator = riskFactorComparator;
        this.riskTrendCalculator = riskTrendCalculator;
    }

    public RiskAnalysisComparisonResponse compareLatest(
            Long propertyId
    ) {
        validateProperty(propertyId);

        List<RiskAnalysis> analyses =
                riskAnalysisRepository
                        .findTop2ByPropertyIdOrderByAnalyzedAtDescIdDesc(
                                propertyId
                        );

        if (analyses.size() < 2) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST
            );
        }

        RiskAnalysis current = analyses.get(0);
        RiskAnalysis previous = analyses.get(1);

        List<RiskFactor> previousFactors =
                riskFactorRepository
                        .findByRiskAnalysisIdOrderByIdAsc(
                                previous.getId()
                        );

        List<RiskFactor> currentFactors =
                riskFactorRepository
                        .findByRiskAnalysisIdOrderByIdAsc(
                                current.getId()
                        );

        List<RiskFactorComparisonResponse> factorChanges =
                riskFactorComparator.compare(
                        previousFactors,
                        currentFactors
                );

        BigDecimal scoreDelta =
                riskTrendCalculator.calculateScoreDelta(
                        previous.getRiskScore(),
                        current.getRiskScore()
                );

        RiskTrend trend =
                riskTrendCalculator.calculateTrend(
                        previous.getRiskScore(),
                        current.getRiskScore()
                );

        boolean policyVersionChanged =
                !Objects.equals(
                        previous.getPolicyVersion(),
                        current.getPolicyVersion()
                );

        return new RiskAnalysisComparisonResponse(
                propertyId,
                RiskAnalysisResponse.from(previous),
                RiskAnalysisResponse.from(current),
                scoreDelta,
                trend,
                policyVersionChanged,
                factorChanges
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