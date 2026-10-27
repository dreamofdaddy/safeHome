package com.safehome.integration;

import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.import=", "server.shutdown=immediate", "safehome.integration.token=test-only-internal-token-32-characters-minimum",
        "safehome.building.service-key="})
class McpErrorIntegrationTest {
    @TempDir static Path historyDirectory;
    @DynamicPropertySource static void isolatedHistory(DynamicPropertyRegistry registry) {
        registry.add("safehome.building.call-history-directory", () -> historyDirectory.toString());
    }
    private static final String TOKEN = "test-only-internal-token-32-characters-minimum";
    private static final String MARKER = "synthetic-sensitive-error-marker";
    @LocalServerPort int port;
    @Autowired JsonMapper mvcMapper;
    @Autowired @Qualifier("mcpServerJsonMapper") JsonMapper mcpMapper;
    private final HttpClient client = HttpClient.newHttpClient();

    @Test void restJobCorrelatesWithBackendHistoryAndRejectsUntrustedIds() throws Exception {
        String body="""
                {"sigunguCd":"11530","bjdongCd":"10700","platGbCd":"0","bun":"0355","ji":"0018",
                "floorGroundType":"ABOVE_GROUND","floorNumber":1,"unitNumber":"101"}
                """;
        var base=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/internal/v1/buildings/resolve"))
                .timeout(Duration.ofSeconds(5)).header("X-SafeHome-Integration-Token",TOKEN)
                .header("Content-Type","application/json");
        var response=client.send(base.copy().header("X-SafeHome-Identification-Id","42")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode());
        String job=response.headers().firstValue("X-SafeHome-Integration-Job-Id").orElseThrow();
        java.util.UUID.fromString(job);
        var events=new java.util.ArrayList<tools.jackson.databind.JsonNode>();
        try(var files=java.nio.file.Files.list(historyDirectory)) {
            for(var file:files.toList()) for(var line:java.nio.file.Files.readAllLines(file)) {
                var event=mvcMapper.readTree(line);
                if(job.equals(event.path("job").path("id").asString())) events.add(event);
            }
        }
        assertEquals(2,events.size()); // No upstream key: no external attempt.
        assertEquals(42,events.get(0).path("job").path("identificationId").asInt());
        assertEquals("REST",events.get(0).path("job").path("entryPoint").asString());
        assertEquals("SERVICE_KEY_NOT_CONFIGURED",events.get(1).path("errorCode").asString());
        for(String invalid:List.of("-1",MARKER,"99999999999999999999999")) {
            var rejected=client.send(base.copy().header("X-SafeHome-Identification-Id",invalid)
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(400,rejected.statusCode()); assertSafe(rejected.body());
        }
    }

    @Test void bothMappersStripThrowableDetailsAndPreserveRpcIdsAndCodes() {
        var error = new McpSchema.JSONRPCResponse.JSONRPCError(-32602, MARKER,
                new IllegalStateException(MARKER, new RuntimeException(MARKER)));
        for (var mapper : List.of(mvcMapper, mcpMapper)) {
            var response = new McpSchema.JSONRPCResponse("2.0", 17, null, error);
            var json = mapper.readTree(mapper.writeValueAsString(response));
            assertEquals(17, json.path("id").asInt());
            assertEquals(-32602, json.path("error").path("code").asInt());
            assertEquals("Invalid params", json.path("error").path("message").asString());
            assertFalse(json.path("error").has("data"));
            assertSafe(json.toString());
            String transportError = mapper.writeValueAsString(new McpError(error));
            assertSafe(transportError);
            assertTrue(mapper.readTree(transportError).path("id").isNull());
        }
    }
    @Test void failedToolContentIsRedactedButSuccessfulEvidenceIsPreserved() {
        var failure = new McpSchema.CallToolResult(List.of(McpSchema.TextContent.builder(MARKER).build()), true,
                Map.of("exception", MARKER), Map.of("debug", MARKER));
        var json = mcpMapper.readTree(mcpMapper.writeValueAsString(failure));
        assertTrue(json.path("isError").asBoolean());
        assertEquals("Tool execution failed", json.path("content").get(0).path("text").asString());
        assertEquals("text", json.path("content").get(0).path("type").asString());
        assertFalse(json.has("structuredContent"));
        assertFalse(json.has("_meta"));
        assertSafe(json.toString());
        var success = new McpSchema.CallToolResult(List.of(McpSchema.TextContent.builder("evidence").build()), false,
                Map.of("status", "UNVERIFIABLE"), Map.of("source", "synthetic"));
        var normal = mcpMapper.readTree(mcpMapper.writeValueAsString(success));
        assertEquals("text", normal.path("content").get(0).path("type").asString());
        assertEquals("evidence", normal.path("content").get(0).path("text").asString());
        assertEquals("UNVERIFIABLE", normal.path("structuredContent").path("status").asString());
        assertEquals("synthetic", normal.path("_meta").path("source").asString());
    }
    @Test void realMcpTransportSanitizesMalformedInputAndKeepsNormalToolFlow() throws Exception {
        var denied = send("{}", null, false);
        assertEquals(401, denied.statusCode());
        var malformed = send("{\"" + MARKER, null, true);
        assertEquals(400, malformed.statusCode());
        assertSafe(malformed.body());
        assertTrue(mvcMapper.readTree(malformed.body()).path("error").has("code"));
        var init = send("""
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{
                "protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"local-test","version":"1"}}}
                """, null, true);
        assertEquals(200, init.statusCode());
        assertTrue(init.body().contains("serverInfo"));
        String session = init.headers().firstValue("Mcp-Session-Id").orElseThrow();
        try {
        var notification = send("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}", session, true);
        assertEquals(202, notification.statusCode());
        var tools = send("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}", session, true);
        assertEquals(200, tools.statusCode());
        assertTrue(tools.body().contains("resolve_building_unit"));
        var unknown = send("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\""+MARKER+"\"}", session, true);
        assertSafe(unknown.body());
        assertTrue(unknown.body().contains("Method not found"));
        assertTrue(unknown.body().contains("\"id\":3"));
        var badArgs = send("""
                {"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"resolve_building_unit",
                "arguments":{"floorGroundType":"%s","floorNumber":"%s"}}}
                """.formatted(MARKER, MARKER), session, true);
        assertSafe(badArgs.body());
        assertTrue(badArgs.body().contains("Tool execution failed") || badArgs.body().contains("Invalid params")
                || badArgs.body().contains("Internal error"));
        var valid = send("""
                {"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"resolve_building_unit",
                "arguments":{"sigunguCd":"11530","bjdongCd":"10700","platGbCd":"0","bun":"0355",
                "ji":"0018","floorGroundType":"ABOVE_GROUND","floorNumber":1,"unitNumber":"101"}}}
                """, session, true);
        assertEquals(200, valid.statusCode());
        assertTrue(valid.body().contains("UNVERIFIABLE"));
        assertTrue(valid.body().contains("SERVICE_KEY_NOT_CONFIGURED"));
        assertEquals("text", mvcMapper.readTree(valid.body()).path("result").path("content").get(0).path("type").asString());
        assertSafe(valid.body());
        } finally {
            var close = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/mcp"))
                    .timeout(Duration.ofSeconds(10)).header("X-SafeHome-Integration-Token", TOKEN)
                    .header("Mcp-Session-Id", session).DELETE().build();
            client.send(close, HttpResponse.BodyHandlers.discarding());
        }
    }
    private Reply send(String body, String session, boolean authorized) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/mcp"))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream");
        if (authorized) request.header("X-SafeHome-Integration-Token", TOKEN);
        if (session != null) request.header("Mcp-Session-Id", session);
        var response = client.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofInputStream());
        try (var input = response.body()) {
            var result = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    if (response.headers().firstValue("Content-Type").orElse("").contains("text/event-stream")) {
                        var reader = new java.io.BufferedReader(new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data:")) return line.substring(5).trim();
                        }
                        return "";
                    }
                    return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
            }).get(10, java.util.concurrent.TimeUnit.SECONDS);
            return new Reply(response.statusCode(), result, response.headers());
        }
    }
    private record Reply(int statusCode, String body, java.net.http.HttpHeaders headers) {}
    private static void assertSafe(String body) {
        assertFalse(body.contains(MARKER));
        for (String detail : List.of("stackTrace", "suppressed", "localizedMessage", "java.lang.", "tools.jackson.", "cause"))
            assertFalse(body.contains(detail), "Internal detail leaked: " + detail);
    }
}
