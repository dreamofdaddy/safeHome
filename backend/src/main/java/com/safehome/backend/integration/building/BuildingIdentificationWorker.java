package com.safehome.backend.integration.building;

import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class BuildingIdentificationWorker {
    private final BuildingIdentificationStore store;
    private final ObjectProvider<BuildingEvidenceClient> clients;

    public BuildingIdentificationWorker(BuildingIdentificationStore store,
                                        ObjectProvider<BuildingEvidenceClient> clients) {
        this.store = store; this.clients = clients;
    }

    @Async("buildingIdentificationExecutor")
    public void process(Long id) {
        BuildingQuery query = store.claim(id);
        if (query == null) return;
        Resolution resolution;
        try {
            BuildingEvidenceClient client = clients.getIfAvailable();
            resolution = client == null ? Resolution.unavailable("INTEGRATION_UNAVAILABLE")
                    : client.resolve(query);
            if (resolution == null) resolution = Resolution.unavailable("INVALID_INTEGRATION_RESPONSE");
        } catch (RuntimeException ignored) {
            // Do not log client exceptions: request headers can contain credentials.
            resolution = Resolution.unavailable("INTEGRATION_UNAVAILABLE");
        }
        boolean failed = resolution.status() == Resolution.Status.UNVERIFIABLE
                && !resolution.collectionComplete()
                && !Objects.equals(resolution.reason(), "PARENT_NOT_CORROBORATED");
        store.finish(id, resolution, failed);
    }
}
