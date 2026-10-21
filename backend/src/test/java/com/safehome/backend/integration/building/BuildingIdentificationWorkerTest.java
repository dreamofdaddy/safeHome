package com.safehome.backend.integration.building;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.mockito.Mockito.*;

class BuildingIdentificationWorkerTest {
    private final BuildingIdentificationStore store = mock(BuildingIdentificationStore.class);
    private final BuildingEvidenceClient client = mock(BuildingEvidenceClient.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<BuildingEvidenceClient> clients = mock(ObjectProvider.class);
    private final BuildingIdentificationWorker worker = new BuildingIdentificationWorker(store, clients);
    private final BuildingQuery query = new BuildingQuery("11710", "10100", "0", "0019", "0000",
            "160", BuildingQuery.Ground.ABOVE_GROUND, 1, "101");

    @Test
    void keepsAnUnverifiableIdentificationAsACompletedCall() {
        when(store.claim(1L)).thenReturn(query);
        when(clients.getIfAvailable()).thenReturn(client);
        Resolution result = new Resolution(Resolution.Status.UNVERIFIABLE, List.of(),
                "PARENT_NOT_CORROBORATED", "MOLIT_BUILDING_HUB", Instant.now(), false);
        when(client.resolve(query, 1L)).thenReturn(result);
        worker.process(1L);
        verify(store).finish(1L, result, false);
    }

    @Test
    void separatesTransportFailureFromFourIdentificationResults() {
        when(store.claim(2L)).thenReturn(query);
        when(clients.getIfAvailable()).thenReturn(client);
        Resolution unavailable = Resolution.unavailable("INTEGRATION_UNAVAILABLE");
        when(client.resolve(query, 2L)).thenReturn(unavailable);
        worker.process(2L);
        verify(store).finish(2L, unavailable, true);
    }

    @Test
    void skipsSupersededPendingWorkBeforeCallingProvider() {
        worker.process(3L);
        verifyNoInteractions(client);
        verify(store, never()).finish(anyLong(), any(), anyBoolean());
    }
}
