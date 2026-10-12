package com.safehome.backend.domain.registry.dto;

import com.safehome.backend.domain.registry.RegistryRight;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RegistryRightSnapshotResponse(
        Long id,
        Long registrySnapshotId,
        String rightType,
        String holderName,
        BigDecimal amount,
        Integer priority,
        LocalDateTime registeredAt,
        String status
) {
    public static RegistryRightSnapshotResponse from(
            RegistryRight registryRight
    ) {
        return new RegistryRightSnapshotResponse(
                registryRight.getId(),
                registryRight.getRegistrySnapshot().getId(),
                registryRight.getRightType(),
                registryRight.getHolderName(),
                registryRight.getAmount(),
                registryRight.getPriority(),
                registryRight.getRegisteredAt(),
                registryRight.getStatus()
        );
    }
}