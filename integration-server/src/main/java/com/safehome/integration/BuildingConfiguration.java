package com.safehome.integration;

import com.safehome.integration.building.BuildingHubClient;
import com.safehome.integration.building.BuildingResolver;
import java.nio.file.Path;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BuildingConfiguration {
    @Bean FileCallHistory fileCallHistory(@Value("${safehome.building.call-history-directory:D:/safehome-data/integration/call-history}") String directory) {
        return new FileCallHistory(Path.of(directory));
    }
    @Bean BuildingResolver buildingResolver(@Value("${safehome.building.service-key}") String key, FileCallHistory history) {
        return new BuildingResolver(BuildingHubClient.recorded(key, history), history);
    }
    @Bean ToolCallbackProvider buildingToolCallbacks(BuildingEndpoints endpoints) {
        return MethodToolCallbackProvider.builder().toolObjects(endpoints).build();
    }
}
