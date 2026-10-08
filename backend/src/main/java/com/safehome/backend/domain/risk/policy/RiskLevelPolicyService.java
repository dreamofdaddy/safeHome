package com.safehome.backend.domain.risk.policy;

import com.safehome.backend.common.code.CodeService;
import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
public class RiskLevelPolicyService {

    private final RiskLevelPolicyRepository repository;
    private final RiskPolicyVersionService policyVersionService;
    private final CodeService codeService;

    public RiskLevelPolicyService(
            RiskLevelPolicyRepository repository,
            RiskPolicyVersionService policyVersionService,
            CodeService codeService
    ) {
        this.repository = repository;
        this.policyVersionService = policyVersionService;
        this.codeService = codeService;
    }

    public RiskLevelPolicy findByScore(BigDecimal score) {

        if (score == null || score.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        String policyVersion =
                policyVersionService.getActivePolicyVersion();

        return repository
                .findApplicablePolicies(policyVersion, score)
                .stream()
                .findFirst()
                .map(policy -> {
                    if (!codeService.isActive(
                            CommonCodes.RISK_LEVEL,
                            policy.getRiskLevel()
                    )) {
                        throw new BusinessException(
                                ErrorCode.INVALID_REQUEST
                        );
                    }

                    return policy;
                })
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.INVALID_REQUEST)
                );
    }

    public String getRiskLevel(BigDecimal score) {
        return findByScore(score).getRiskLevel();
    }
}