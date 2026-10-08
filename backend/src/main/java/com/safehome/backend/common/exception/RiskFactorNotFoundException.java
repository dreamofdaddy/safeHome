package com.safehome.backend.common.exception;

public class RiskFactorNotFoundException extends BusinessException {

    public RiskFactorNotFoundException(Long id) {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }
}