package com.safehome.backend.integration.building;

import com.safehome.backend.common.response.ApiResponse;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "safehome.integration.enabled", havingValue = "true")
public class BuildingEvidenceClient {
    private final RestClient client;
    public BuildingEvidenceClient(
            @Value("${safehome.integration.base-url:http://127.0.0.1:8090}") String baseUrl,
            @Value("${safehome.integration.token:}") String token) {
        URI base = URI.create(baseUrl);
        if (base.getHost() == null || base.getUserInfo() != null || base.getQuery() != null
                || base.getFragment() != null || !("http".equals(base.getScheme()) || "https".equals(base.getScheme()))
                || token.length() < 32) throw new IllegalArgumentException("Invalid Integration Server configuration");
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(125));
        client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory)
                .defaultHeader("X-SafeHome-Integration-Token", token).build();
    }
    public Resolution resolve(BuildingQuery query) {
        try {
            ApiResponse<Resolution> response = client.post().uri("/internal/v1/buildings/resolve")
                    .body(query).retrieve().body(new ParameterizedTypeReference<ApiResponse<Resolution>>() {});
            if (response == null || !response.success() || response.data() == null
                    || response.data().status() == null || response.data().queriedAt() == null)
                return Resolution.unavailable("INVALID_INTEGRATION_RESPONSE");
            Resolution data = response.data();
            if (data.status() != Resolution.Status.UNVERIFIABLE && !data.collectionComplete())
                return Resolution.unavailable("INCOMPLETE_INTEGRATION_RESPONSE");
            int count = data.candidates().size();
            if ((data.status() == Resolution.Status.MATCHED && count != 1)
                    || (data.status() == Resolution.Status.NOT_FOUND && count != 0)
                    || (data.status() == Resolution.Status.AMBIGUOUS && count < 2))
                return Resolution.unavailable("INCONSISTENT_INTEGRATION_RESPONSE");
            return data;
        } catch (org.springframework.web.client.RestClientException | IllegalArgumentException e) {
            // Never log the exception: request headers may contain internal credentials.
            return Resolution.unavailable("INTEGRATION_UNAVAILABLE");
        }
    }
}
