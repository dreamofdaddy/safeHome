package com.safehome.backend.domain.document.parser;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VerifiedRegistryDocument(
    String registryPropertyNumber,
    String address,
    String roadAddress,
    String buildingName,
    String unitNumber,
    LocalDateTime observedAt,
    List<Owner> owners,
    List<Right> rights
) {
    public record Owner(String name, String maskedIdentifier,
                        BigDecimal ownershipRatio, LocalDateTime registeredAt) {}
    public record Right(String type, String holderName, BigDecimal amount,
                        Integer priority, LocalDateTime registeredAt, String status) {}
}
