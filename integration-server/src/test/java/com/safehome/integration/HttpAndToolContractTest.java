package com.safehome.integration;

import com.safehome.integration.building.BuildingHubClient;
import com.safehome.integration.building.BuildingResolver;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HttpAndToolContractTest {
    private static final String TOKEN = "test-only-internal-token-32-characters-minimum";
    private static final String QUERY = """
            {"sigunguCd":"11530","bjdongCd":"10700","platGbCd":"0",
             "bun":"0355","ji":"0018","dongName":null,"floorGroundType":"ABOVE_GROUND",
             "floorNumber":1,"unitNumber":"101"}
            """;
    private BuildingEndpoints endpoints() {
        return new BuildingEndpoints(new BuildingResolver(new BuildingHubClient("")));
    }
    @Test void authAndUnavailableAreSeparateFromRisk() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(endpoints())
                .setControllerAdvice(new ApiErrors())
                .addFilters(new InternalAccessFilter(TOKEN)).build();
        mvc.perform(post("/internal/v1/buildings/resolve").contentType("application/json").content(QUERY))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/v1/buildings/resolve")
                        .header("X-SafeHome-Integration-Token", TOKEN)
                        .contentType("application/json").content(QUERY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UNVERIFIABLE"))
                .andExpect(jsonPath("$.data.reason").value("SERVICE_KEY_NOT_CONFIGURED"))
                .andExpect(jsonPath("$.data.collectionComplete").value(false));
        mvc.perform(post("/internal/v1/buildings/resolve")
                        .header("X-SafeHome-Integration-Token", TOKEN)
                        .contentType("application/json").content(QUERY.replace("ABOVE_GROUND", "UNKNOWN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INTEGRATION-001"));
        mvc.perform(post("/mcp").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }
    @Test void missingServerTokenFailsClosed() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(endpoints())
                .addFilters(new InternalAccessFilter("")).build();
        mvc.perform(post("/internal/v1/buildings/resolve")
                        .contentType("application/json").content(QUERY))
                .andExpect(status().isServiceUnavailable());
    }
    @Test void toolIsRegisteredWithStableName() {
        var callbacks = MethodToolCallbackProvider.builder().toolObjects(endpoints()).build().getToolCallbacks();
        assertEquals(1, callbacks.length);
        assertEquals("resolve_building_unit", callbacks[0].getToolDefinition().name());
    }
}
