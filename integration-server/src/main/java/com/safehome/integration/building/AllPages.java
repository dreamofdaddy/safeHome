package com.safehome.integration.building;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AllPages {
    public record Page(int pageNumber, int totalCount, List<BuildingUnit> units) {
        public Page { units = List.copyOf(units); }
    }
    @FunctionalInterface public interface Fetcher { Page fetch(int page); }
    public List<BuildingUnit> collect(Fetcher fetcher, int maxPages, int maxRecords) {
        List<BuildingUnit> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        Integer expected = null;
        for (int pageNumber = 1; pageNumber <= maxPages; pageNumber++) {
            Page page = fetcher.fetch(pageNumber);
            if (page.pageNumber() != pageNumber || page.totalCount() < 0
                    || page.totalCount() > maxRecords
                    || (expected != null && expected != page.totalCount())) fail();
            expected = page.totalCount();
            if (page.units().isEmpty() && result.size() < expected) fail();
            for (BuildingUnit unit : page.units()) {
                if (unit.managementNumber() == null || unit.managementNumber().isBlank()
                        || !ids.add(unit.managementNumber())) fail();
                result.add(unit);
            }
            if (result.size() > expected) fail();
            if (result.size() == expected) return List.copyOf(result);
        }
        throw new UpstreamException("PAGE_LIMIT_EXCEEDED", false);
    }
    private static void fail() { throw new UpstreamException("INCONSISTENT_PAGINATION", false); }
}
