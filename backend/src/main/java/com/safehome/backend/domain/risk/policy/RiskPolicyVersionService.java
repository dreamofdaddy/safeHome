package com.safehome.backend.domain.risk.policy;

import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RiskPolicyVersionService {

    private final RiskPolicyVersionRepository repository;

    public RiskPolicyVersionService(
            RiskPolicyVersionRepository repository
    ) {
        this.repository = repository;
    }

    public RiskPolicyVersion findActive() {
        return repository
                .findFirstByUseYnOrderByIdDesc("Y")
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.INVALID_REQUEST)
                );
    }

    public String getActivePolicyVersion() {
        return findActive().getPolicyVersion();
    }
}