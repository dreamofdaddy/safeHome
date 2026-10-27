package com.safehome.integration.building;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BuildingHubTransportTest {
    @Test void boundsChunksBeforeRetainingThemAndCancelsImmediately() {
        var subscriber = new LimitedBodySubscriber(4, -1);
        var subscription = new TestSubscription();
        subscriber.onSubscribe(subscription);
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[4])));
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[1])));
        subscriber.onComplete();
        var error = assertThrows(CompletionException.class, () -> subscriber.getBody().toCompletableFuture().join());
        assertInstanceOf(LimitedBodySubscriber.ResponseTooLargeException.class, error.getCause());
        assertTrue(subscription.cancelled);
    }
    @Test void declaredOversizeBodyIsCancelledWithoutDemand() {
        var subscriber = new LimitedBodySubscriber(4, 5);
        var subscription = new TestSubscription();
        subscriber.onSubscribe(subscription);
        assertTrue(subscription.cancelled);
        assertEquals(0, subscription.requested);
        assertTrue(subscriber.getBody().toCompletableFuture().isCompletedExceptionally());
    }
    @Test void acceptsExactlyTheLimitAcrossMultipleBuffers() {
        var subscriber = new LimitedBodySubscriber(4, -1);
        var subscription = new TestSubscription();
        subscriber.onSubscribe(subscription);
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[]{1, 2}), ByteBuffer.wrap(new byte[]{3, 4})));
        subscriber.onComplete();
        assertArrayEquals(new byte[]{1, 2, 3, 4}, subscriber.getBody().toCompletableFuture().join());
        assertFalse(subscription.cancelled);
    }
    @Test void realHttpRejectsDeclaredAndChunkedOversizeWithoutRetryAndPreservesStatusErrors() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/declared", exchange -> {
            exchange.sendResponseHeaders(200, BuildingHubClient.MAX_RESPONSE_BYTES + 1L);
            exchange.close();
        });
        server.createContext("/chunked", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (var body = exchange.getResponseBody()) {
                byte[] chunk = new byte[8192];
                for (int n = 0; n < 400; n++) body.write(chunk);
            } catch (java.io.IOException expectedCancellation) { /* Client rejects the stream. */ }
        });
        server.createContext("/boundary", exchange -> {
            exchange.sendResponseHeaders(200, BuildingHubClient.MAX_RESPONSE_BYTES);
            try (var body = exchange.getResponseBody()) { body.write(new byte[BuildingHubClient.MAX_RESPONSE_BYTES]); }
        });
        server.createContext("/unavailable", exchange -> {
            exchange.sendResponseHeaders(503, -1); exchange.close();
        });
        server.start();
        try {
            var transport = BuildingHubClient.defaultTransport();
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            for (String path : List.of("/declared", "/chunked")) {
                var calls = new AtomicInteger();
                var client = new BuildingHubClient("synthetic-key", uri -> {
                    calls.incrementAndGet(); return transport.get(URI.create(base + path));
                });
                var query = new BuildingQuery("11530", "10700", "0", "0355", "0018", null,
                        BuildingQuery.Ground.ABOVE_GROUND, 1, "101");
                var error = assertThrows(UpstreamException.class, () -> client.fetchAll(
                        BuildingHubClient.Operation.EXPOS, query, System.nanoTime() + 30_000_000_000L));
                assertEquals("EXPOS_PAGE_1_RESPONSE_SIZE", error.getMessage());
                assertFalse(error.retryable());
                assertEquals(1, calls.get());
                assertNull(error.getCause());
            }
            assertEquals(BuildingHubClient.MAX_RESPONSE_BYTES, transport.get(URI.create(base + "/boundary")).length);
            var error = assertThrows(UpstreamException.class, () -> transport.get(URI.create(base + "/unavailable")));
            assertEquals("UPSTREAM_HTTP_503", error.getMessage());
            assertTrue(error.retryable());
        } finally { server.stop(0); }
    }
    @Test void retriesAnEmptyHttp200OnceAndAcceptsTheNextCompletePage() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var calls = new AtomicInteger();
        String xml = "<response><header><resultCode>00</resultCode></header><body>"
                + "<pageNo>1</pageNo><totalCount>0</totalCount><items/></body></response>";
        server.createContext("/", exchange -> {
            if (calls.incrementAndGet() == 1) {
                exchange.sendResponseHeaders(200, -1); exchange.close();
            } else {
                byte[] body = xml.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            }
        });
        server.start();
        try {
            var events = new java.util.ArrayList<CallHistory.Event>();
            var transport = BuildingHubClient.defaultTransport();
            var target = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
            var client = new BuildingHubClient("synthetic-key", uri -> transport.get(target), events::add);
            var query = new BuildingQuery("11530", "10700", "0", "0355", "0018", null,
                    BuildingQuery.Ground.ABOVE_GROUND, 1, "101");
            assertTrue(client.fetchAll(BuildingHubClient.Operation.EXPOS, query,
                    System.nanoTime() + 30_000_000_000L).isEmpty());
            assertEquals(2, calls.get());
            assertEquals("EMPTY_RESPONSE", events.getFirst().errorCode());
            assertEquals(CallHistory.Outcome.SUCCESS, events.getLast().outcome());
        } finally { server.stop(0); }
    }
    @Test void repeatedEmptyResponsesStopAfterFourAttemptsAndNeverMeanNotFound() {
        var calls = new AtomicInteger();
        var events = new java.util.ArrayList<CallHistory.Event>();
        var client = new BuildingHubClient("synthetic-key", uri -> {
            calls.incrementAndGet(); return new byte[0];
        }, events::add);
        var query = new BuildingQuery("11530", "10700", "0", "0355", "0018", null,
                BuildingQuery.Ground.ABOVE_GROUND, 1, "101");
        var result = new BuildingResolver(client, events::add).resolve(query);
        assertEquals(4, calls.get());
        assertEquals(Resolution.Status.UNVERIFIABLE, result.status());
        assertFalse(result.collectionComplete());
        assertEquals("EXPOS_PAGE_1_EMPTY_RESPONSE", result.reason());
        assertEquals(4, events.stream().filter(e -> e.kind() == CallHistory.Kind.API_ATTEMPT).count());
        assertEquals("EXPOS_PAGE_1_EMPTY_RESPONSE", events.getLast().errorCode());
    }
    private static final class TestSubscription implements Flow.Subscription {
        boolean cancelled;
        long requested;
        @Override public void request(long n) { requested = n; }
        @Override public void cancel() { cancelled = true; }
    }
}
