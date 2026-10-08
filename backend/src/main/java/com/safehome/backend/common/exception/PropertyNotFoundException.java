package com.safehome.backend.common.exception;

public class PropertyNotFoundException extends BusinessException {

    public PropertyNotFoundException(Long id) {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }
}