package com.safehome.backend.domain.property.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PropertyOwnerCreateRequest(

        @NotNull
        Long propertyId,

        @NotNull
        Long userId,

        @DecimalMin(value = "0.01")
        @DecimalMax(value = "100.00")
        @Digits(integer = 3, fraction = 2)
        BigDecimal ownershipRatio
) {
}