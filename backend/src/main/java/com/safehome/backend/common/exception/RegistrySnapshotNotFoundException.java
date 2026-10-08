package com.safehome.backend.common.exception;

public class RegistrySnapshotNotFoundException extends BusinessException {

    public RegistrySnapshotNotFoundException(Long id) {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }
}