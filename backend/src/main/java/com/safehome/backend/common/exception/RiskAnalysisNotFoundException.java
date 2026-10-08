package com.safehome.backend.common.exception;

public class RiskAnalysisNotFoundException extends BusinessException {

    public RiskAnalysisNotFoundException(Long id) {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }
}