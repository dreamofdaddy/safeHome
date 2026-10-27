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
                        BigDecimal ownershipRatio, LocalDateTime registeredAt, String ownershipShare,
                        String ownershipType,String priorityNumber,String ownerAddress,BigDecimal acquisitionAmount, String entityType) {
        public Owner(String name,String maskedIdentifier,BigDecimal ownershipRatio,LocalDateTime registeredAt,String ownershipShare,
                     String ownershipType,String priorityNumber,String ownerAddress,BigDecimal acquisitionAmount) {
            this(name,maskedIdentifier,ownershipRatio,registeredAt,ownershipShare,ownershipType,priorityNumber,ownerAddress,acquisitionAmount,"PERSON");
        }
        public Owner(String name,String maskedIdentifier,BigDecimal ownershipRatio,LocalDateTime registeredAt,String ownershipShare) {
            this(name,maskedIdentifier,ownershipRatio,registeredAt,ownershipShare,"UNKNOWN",null,null,null);
        }
        public Owner(String name,String maskedIdentifier,BigDecimal ownershipRatio,LocalDateTime registeredAt) {
            this(name,maskedIdentifier,ownershipRatio,registeredAt,null);
        }
    }
    public record Right(String type, String holderName, BigDecimal amount,
                        Integer priority, LocalDateTime registeredAt, String status, String sourceName) {
        public Right(String type,String holderName,BigDecimal amount,Integer priority,LocalDateTime registeredAt,String status) {
            this(type,holderName,amount,priority,registeredAt,status,null);
        }
    }
}
