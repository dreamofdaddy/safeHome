package com.safehome.backend.domain.property.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PropertyOwnerCreateRequest(
        @NotNull Long propertyId,
        @NotNull Long registrySnapshotId,
        @NotBlank @Size(max = 200) String ownerName,
        @NotBlank @Pattern(regexp = "[0-9]{6}-\\*{7}") String ownerRrnMasked,
        @DecimalMin("0.01") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2)
        BigDecimal ownershipRatio,
        LocalDateTime registeredAt
) {}
