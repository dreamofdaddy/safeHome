package com.safehome.backend.domain.document.parser;

import java.math.BigDecimal;
import java.util.List;

public record ParsedRegistryDocument(
    String registryPropertyNumber,
    String address,
    String roadAddress,
    String buildingName,
    String unitNumber,
    String observedAt,
    List<Owner> owners,
    List<Right> rights
) {
    public record Owner(String name, String maskedIdentifier,
                        BigDecimal ownershipRatio, String registeredAt) {}
    public record Right(String type, String holderName, BigDecimal amount,
                        Integer priority, String registeredAt, String status) {}
}
