package com.safehome.backend.domain.risk;

import com.safehome.backend.domain.risk.dto.SafetyAnalysisResponse;
import com.safehome.backend.domain.risk.dto.SafetyReanalysisDecisionResponse;
import com.safehome.backend.domain.risk.dto.SafetyReanalysisExecutionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SafetyReanalysisService {

    private final SafetyReanalysisDecisionService
            safetyReanalysisDecisionService;

    private final SafetyAnalysisService safetyAnalysisService;

    public SafetyReanalysisService(
            SafetyReanalysisDecisionService
                    safetyReanalysisDecisionService,
            SafetyAnalysisService safetyAnalysisService
    ) {
        this.safetyReanalysisDecisionService =
                safetyReanalysisDecisionService;

        this.safetyAnalysisService =
                safetyAnalysisService;
    }

    @Transactional
    public SafetyReanalysisExecutionResponse reanalyzeIfRequired(
            Long propertyId
    ) {
        SafetyReanalysisDecisionResponse decision =
                safetyReanalysisDecisionService.decide(
                        propertyId
                );

        if (!decision.required()) {
            return new SafetyReanalysisExecutionResponse(
                    propertyId,
                    false,
                    decision.reason(),
                    null
            );
        }

        SafetyAnalysisResponse analysis =
                safetyAnalysisService.analyze(
                        propertyId
                );

        return new SafetyReanalysisExecutionResponse(
                propertyId,
                true,
                decision.reason(),
                analysis
        );
    }
}