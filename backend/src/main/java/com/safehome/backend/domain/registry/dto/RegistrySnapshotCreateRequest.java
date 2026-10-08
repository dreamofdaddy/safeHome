package com.safehome.backend.domain.registry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record RegistrySnapshotCreateRequest(

        @NotNull
        Long propertyId,

        @NotBlank
        @Size(max = 100)
        String source,

        @NotNull
        LocalDateTime observedAt,

        @Size(max = 128)
        String documentHash
) {
}