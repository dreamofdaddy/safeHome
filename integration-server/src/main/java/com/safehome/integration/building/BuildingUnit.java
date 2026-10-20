package com.safehome.integration.building;

public record BuildingUnit(String managementNumber, String parentManagementNumber,
                           String sigunguCd, String bjdongCd, String platGbCd,
                           String bun, String ji, String address, String roadAddress,
                           String dongName, BuildingQuery.Ground floorGroundType,
                           Integer floorNumber, String unitName, String purpose) {}
