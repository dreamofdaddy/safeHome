package com.safehome.backend.integration.building;
import com.safehome.backend.domain.listing.AddressSearchEvidence;
public record BuildingLookupRequest(AddressSearchEvidence addressSearchEvidence, String detailAddress) {}
