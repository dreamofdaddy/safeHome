package com.safehome.integration.building;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class BuildingMatcher {
    private static final Pattern FLOOR_UNIT = Pattern.compile("^(지하|지상|B)?(?:제)?([0-9]+)층(?:제)?(.+)$");
    private static final Pattern BASEMENT_UNIT = Pattern.compile("^지층(?:제)?(.+)$");
    private record UnitLabel(String unit, BuildingQuery.Ground ground, Integer floor) {}

    // Remove only established decorations. Never discard letters, basement marks, or embedded punctuation.
    static String normalize(String raw, String suffix) {
        if (raw == null) return "";
        String value = Normalizer.normalize(raw, Normalizer.Form.NFKC).strip()
                .replaceAll("\\s+", "");
        if (value.startsWith("제")) value = value.substring(1);
        if (value.endsWith(suffix)) value = value.substring(0, value.length() - suffix.length());
        if (value.matches("[0-9]+")) value = value.replaceFirst("^0+(?!$)", "");
        return value;
    }
    private static UnitLabel unitLabel(String raw) {
        String compact = Normalizer.normalize(raw, Normalizer.Form.NFKC).strip().replaceAll("\\s+", "");
        if (compact.startsWith("제")) compact = compact.substring(1);
        var floor = FLOOR_UNIT.matcher(compact);
        if (floor.matches()) {
            BuildingQuery.Ground ground = "지하".equals(floor.group(1)) || "B".equals(floor.group(1))
                    ? BuildingQuery.Ground.BELOW_GROUND : BuildingQuery.Ground.ABOVE_GROUND;
            try {
                return new UnitLabel(normalize(floor.group(3), "호"), ground,
                        Integer.parseInt(floor.group(2)));
            } catch (NumberFormatException ignored) {
                return new UnitLabel("", null, null);
            }
        }
        var basement = BASEMENT_UNIT.matcher(compact);
        if (basement.matches()) {
            return new UnitLabel(normalize(basement.group(1), "호"),
                    BuildingQuery.Ground.BELOW_GROUND, null);
        }
        return new UnitLabel(normalize(raw, "호"), null, null);
    }
    public Resolution match(BuildingQuery query, List<BuildingUnit> units,
                            boolean complete, Instant queriedAt) {
        if (!complete) return Resolution.unavailable("INCOMPLETE_COLLECTION");
        List<BuildingUnit> matched = new ArrayList<>();
        boolean unknown = false;
        for (BuildingUnit unit : units) {
            // Missing cadastral fields cannot establish exclusion.
            String[][] pairs = {{query.sigunguCd(), unit.sigunguCd()}, {query.bjdongCd(), unit.bjdongCd()},
                    {query.platGbCd(), unit.platGbCd()}, {query.bun(), unit.bun()}, {query.ji(), unit.ji()}};
            boolean excluded = false;
            boolean missing = false;
            for (String[] pair : pairs) {
                if (pair[1] == null || pair[1].isBlank()) missing = true;
                else if (!pair[0].equals(pair[1])) excluded = true;
            }
            if (excluded) continue;
            if (query.dongName() != null) {
                if (unit.dongName() == null || unit.dongName().isBlank()) missing = true;
                else if (!normalize(query.dongName(), "동").equals(normalize(unit.dongName(), "동"))) continue;
            }
            UnitLabel label = unit.unitName() == null || unit.unitName().isBlank()
                    ? null : unitLabel(unit.unitName());
            if (label != null && !normalize(query.unitNumber(), "호").equals(label.unit())) continue;
            if (label != null && ((label.ground() != null && unit.floorGroundType() != null
                    && label.ground() != unit.floorGroundType())
                    || (label.floor() != null && unit.floorNumber() != null
                    && !label.floor().equals(unit.floorNumber())))) {
                unknown = true;
                continue;
            }
            if (unit.floorGroundType() != null && unit.floorGroundType() != query.floorGroundType()) continue;
            if (unit.floorNumber() != null && !unit.floorNumber().equals(query.floorNumber())) continue;
            if (missing || unit.floorGroundType() == null || unit.floorNumber() == null
                    || label == null
                    || unit.managementNumber() == null || unit.managementNumber().isBlank()
                    || unit.parentManagementNumber() == null || unit.parentManagementNumber().isBlank()
                    || unit.address() == null || unit.address().isBlank()) unknown = true;
            else matched.add(unit);
        }
        if (unknown) return new Resolution(Resolution.Status.UNVERIFIABLE, List.of(),
                "UNINTERPRETABLE_CANDIDATE", "MOLIT_BUILDING_HUB", queriedAt, true);
        Resolution.Status status = matched.isEmpty() ? Resolution.Status.NOT_FOUND
                : matched.size() == 1 ? Resolution.Status.MATCHED : Resolution.Status.AMBIGUOUS;
        return new Resolution(status, matched, null, "MOLIT_BUILDING_HUB", queriedAt, true);
    }
}
