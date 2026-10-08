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
public class RiskFactorPolicyService {

    private final RiskFactorPolicyRepository repository;
    private final RiskPolicyVersionService policyVersionService;
    private final CodeService codeService;

    public RiskFactorPolicyService(
            RiskFactorPolicyRepository repository,
            RiskPolicyVersionService policyVersionService,
            CodeService codeService
    ) {
        this.repository = repository;
        this.policyVersionService = policyVersionService;
        this.codeService = codeService;
    }

    public RiskFactorPolicy findActive(
            String factorType,
            String severity
    ) {
        validateCode(factorType, severity);

        String policyVersion =
                policyVersionService.getActivePolicyVersion();

        return repository
                .findFirstByPolicyVersionAndFactorTypeAndSeverityAndUseYnOrderByIdDesc(
                        policyVersion,
                        factorType,
                        severity,
                        "Y"
                )
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.INVALID_REQUEST
                        )
                );
    }

    public BigDecimal getScore(
            String factorType,
            String severity
    ) {
        return findActive(factorType, severity)
                .getScore();
    }

    private void validateCode(
            String factorType,
            String severity
    ) {
        if (!codeService.isActive(
                CommonCodes.RISK_FACTOR_TYPE,
                factorType
        )) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST
            );
        }

        if (!codeService.isActive(
                CommonCodes.RISK_SEVERITY,
                severity
        )) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST
            );
        }
    }
}