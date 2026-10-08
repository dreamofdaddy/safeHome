package com.safehome.backend.common.exception;

public class ContractNotFoundException extends BusinessException {

    public ContractNotFoundException(Long id) {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }
}