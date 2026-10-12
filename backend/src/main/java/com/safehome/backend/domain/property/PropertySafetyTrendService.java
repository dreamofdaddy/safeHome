package com.safehome.backend.domain.property;

import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import com.safehome.backend.common.exception.PropertyNotFoundException;
import com.safehome.backend.domain.property.dto.PropertySafetyTrendResponse;
import com.safehome.backend.domain.risk.RiskAnalysis;
import com.safehome.backend.domain.risk.RiskAnalysisRepository;
import com.safehome.backend.domain.risk.comparison.RiskTrend;
import com.safehome.backend.domain.risk.comparison.RiskTrendCalculator;
import com.safehome.backend.domain.risk.dto.RiskAnalysisResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class PropertySafetyTrendService {

    private final PropertyRepository propertyRepository;
    private final RiskAnalysisRepository riskAnalysisRepository;
    private final RiskTrendCalculator riskTrendCalculator;

    public PropertySafetyTrendService(
            PropertyRepository propertyRepository,
            RiskAnalysisRepository riskAnalysisRepository,
            RiskTrendCalculator riskTrendCalculator
    ) {
        this.propertyRepository = propertyRepository;
        this.riskAnalysisRepository = riskAnalysisRepository;
        this.riskTrendCalculator = riskTrendCalculator;
    }

    public PropertySafetyTrendResponse getTrend(
            Long propertyId
    ) {
        validateProperty(propertyId);

        List<RiskAnalysis> analyses =
                riskAnalysisRepository
                        .findByPropertyIdOrderByAnalyzedAtDescIdDesc(
                                propertyId
                        );

        List<RiskAnalysisResponse> history =
                analyses.stream()
                        .map(RiskAnalysisResponse::from)
                        .toList();

        if (analyses.isEmpty()) {
            return new PropertySafetyTrendResponse(
                    propertyId,
                    0,
                    null,
                    null,
                    null,
                    null,
                    false,
                    history
            );
        }

        RiskAnalysis current = analyses.get(0);

        if (analyses.size() == 1) {
            return new PropertySafetyTrendResponse(
                    propertyId,
                    1,
                    RiskAnalysisResponse.from(current),
                    null,
                    null,
                    null,
                    false,
                    history
            );
        }

        RiskAnalysis previous = analyses.get(1);

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

        return new PropertySafetyTrendResponse(
                propertyId,
                analyses.size(),
                RiskAnalysisResponse.from(current),
                RiskAnalysisResponse.from(previous),
                scoreDelta,
                trend,
                policyVersionChanged,
                history
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