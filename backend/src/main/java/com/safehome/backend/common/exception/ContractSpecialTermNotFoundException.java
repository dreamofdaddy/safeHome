package com.safehome.backend.common.exception;

public class ContractSpecialTermNotFoundException extends BusinessException {

    public ContractSpecialTermNotFoundException(Long id) {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }
}