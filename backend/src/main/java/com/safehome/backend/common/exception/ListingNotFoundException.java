package com.safehome.backend.common.exception;

public class ListingNotFoundException extends BusinessException {

    public ListingNotFoundException(Long id) {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }
}