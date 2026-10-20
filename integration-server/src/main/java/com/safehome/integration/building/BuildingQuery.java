package com.safehome.integration.building;

public record BuildingQuery(String sigunguCd, String bjdongCd, String platGbCd,
                            String bun, String ji, String dongName,
                            Ground floorGroundType, Integer floorNumber, String unitNumber) {
    public enum Ground { ABOVE_GROUND, BELOW_GROUND }
    public BuildingQuery {
        if (!digits(sigunguCd, 5) || !digits(bjdongCd, 5)
                || platGbCd == null || !platGbCd.matches("[01]")
                || !digits(bun, 4) || !digits(ji, 4)
                || floorGroundType == null || floorNumber == null || floorNumber < 1
                || floorNumber > 999 || unitNumber == null || unitNumber.isBlank()
                || unitNumber.length() > 100 || (dongName != null && dongName.length() > 100)) {
            throw new IllegalArgumentException("Invalid building identification");
        }
        if (dongName != null && dongName.isBlank()) dongName = null;
    }
    private static boolean digits(String value, int count) {
        return value != null && value.matches("[0-9]{" + count + "}");
    }
}
