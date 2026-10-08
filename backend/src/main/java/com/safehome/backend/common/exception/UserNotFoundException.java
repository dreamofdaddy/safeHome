package com.safehome.backend.common.exception;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(Long id) {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }
}