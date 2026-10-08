package com.safehome.backend.domain.property.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PropertyUpdateRequest(

    @NotBlank
    @Size(max = 500)
    String address,

    @Size(max = 500)
    String roadAddress,

    @Size(max = 200)
    String buildingName,

    @Size(max = 50)
    String buildingType,

    @Size(max = 100)
    String unitNumber,

    @Size(max = 20)
    String postalCode

) {
}