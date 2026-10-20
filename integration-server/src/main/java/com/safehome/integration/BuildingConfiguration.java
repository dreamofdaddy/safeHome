package com.safehome.integration;

import com.safehome.integration.building.BuildingHubClient;
import com.safehome.integration.building.BuildingResolver;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BuildingConfiguration {
    @Bean BuildingResolver buildingResolver(@Value("${safehome.building.service-key}") String key) {
        return new BuildingResolver(new BuildingHubClient(key));
    }
    @Bean ToolCallbackProvider buildingToolCallbacks(BuildingEndpoints endpoints) {
        return MethodToolCallbackProvider.builder().toolObjects(endpoints).build();
    }
}
