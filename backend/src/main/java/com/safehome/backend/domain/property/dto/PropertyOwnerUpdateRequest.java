package com.safehome.backend.domain.property.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public record PropertyOwnerUpdateRequest(

        @DecimalMin(value = "0.01")
        @DecimalMax(value = "100.00")
        @Digits(integer = 3, fraction = 2)
        BigDecimal ownershipRatio
) {
}