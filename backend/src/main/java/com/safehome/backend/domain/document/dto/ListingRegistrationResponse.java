package com.safehome.backend.domain.document.dto;

import com.safehome.backend.domain.listing.dto.ListingResponse;

public record ListingRegistrationResponse(ListingResponse listing,
    RegistryDocumentResponse document, String message) {}
