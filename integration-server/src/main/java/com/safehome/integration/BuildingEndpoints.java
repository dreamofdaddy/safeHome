package com.safehome.integration;

import com.safehome.integration.building.BuildingQuery;
import com.safehome.integration.building.BuildingResolver;
import com.safehome.integration.building.Resolution;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.bind.annotation.*;

@RestController
public class BuildingEndpoints {
    private final BuildingResolver resolver;
    public BuildingEndpoints(BuildingResolver resolver) { this.resolver = resolver; }
    public record ApiResult(boolean success, Resolution data) {}
    @PostMapping("/internal/v1/buildings/resolve")
    public ApiResult resolve(@RequestBody BuildingQuery query) {
        return new ApiResult(true, resolver.resolve(query));
    }
    @Tool(name = "resolve_building_unit",
          description = "Identify a Korean building registry unit by cadastral codes, building, ground type, floor and unit. Returns evidence only; never judges transaction safety.")
    public Resolution resolveBuildingUnit(String sigunguCd, String bjdongCd, String platGbCd,
                                          String bun, String ji,
                                          @ToolParam(required = false) String dongName,
                                          BuildingQuery.Ground floorGroundType, Integer floorNumber,
                                          String unitNumber) {
        return resolver.resolve(new BuildingQuery(sigunguCd, bjdongCd, platGbCd, bun, ji,
                dongName, floorGroundType, floorNumber, unitNumber));
    }
}
