package com.safehome.integration;

import com.safehome.integration.building.BuildingQuery;
import com.safehome.integration.building.BuildingResolver;
import com.safehome.integration.building.Resolution;
import com.safehome.integration.building.CallHistory;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.bind.annotation.*;

@RestController
public class BuildingEndpoints {
    private final BuildingResolver resolver;
    public BuildingEndpoints(BuildingResolver resolver) { this.resolver = resolver; }
    public record ApiResult(boolean success, Resolution data) {}
    @PostMapping("/internal/v1/buildings/resolve")
    public ApiResult resolve(@RequestBody BuildingQuery query,
                             @RequestHeader(name = "X-SafeHome-Identification-Id", required = false) Long identificationId,
                             HttpServletResponse response) {
        if (identificationId != null && identificationId < 1) throw new IllegalArgumentException("Invalid identification id");
        var job = CallHistory.Job.create(identificationId, CallHistory.EntryPoint.REST);
        response.setHeader("X-SafeHome-Integration-Job-Id", job.id().toString());
        return new ApiResult(true, resolver.resolve(query, job));
    }
    @Tool(name = "resolve_building_unit",
          description = "Identify a Korean building registry unit by cadastral codes, building, ground type, floor and unit. Returns evidence only; never judges transaction safety.")
    public Resolution resolveBuildingUnit(String sigunguCd, String bjdongCd, String platGbCd,
                                          String bun, String ji,
                                          @ToolParam(required = false) String dongName,
                                          BuildingQuery.Ground floorGroundType, Integer floorNumber,
                                          String unitNumber) {
        return resolver.resolve(new BuildingQuery(sigunguCd, bjdongCd, platGbCd, bun, ji,
                dongName, floorGroundType, floorNumber, unitNumber),
                CallHistory.Job.create(null, CallHistory.EntryPoint.MCP));
    }
}
