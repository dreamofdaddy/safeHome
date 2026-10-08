package com.safehome.backend.domain.registry.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RegistryRightCreateRequest(

        @NotNull
        Long registrySnapshotId,

        @NotBlank
        @Size(max = 50)
        String rightType,

        @Size(max = 200)
        String holderName,

        @Digits(integer = 15, fraction = 0)
        BigDecimal amount,

        @Min(1)
        Integer priority,

        LocalDateTime registeredAt
) {
}