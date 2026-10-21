package com.safehome.integration;

import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.JSONRPCResponse.JSONRPCError;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.util.List;
import java.util.Map;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerJsonMapperAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

/** Applies to both MVC transport errors and the MCP JSON/SSE message mapper. */
@Configuration
public class McpErrorConfiguration {
    @Bean SimpleModule safeMcpErrors() {
        var module = new SimpleModule("safe-mcp-errors");
        module.addSerializer(JSONRPCError.class, new StdSerializer<JSONRPCError>(JSONRPCError.class) {
            @Override public void serialize(JSONRPCError error, JsonGenerator gen, SerializationContext context) {
                context.writeValue(gen, safeError(error));
            }
        });
        module.addSerializer(McpError.class, new StdSerializer<McpError>(McpError.class) {
            @Override public void serialize(McpError error, JsonGenerator gen, SerializationContext context) {
                gen.writeStartObject();
                gen.writeStringProperty("jsonrpc", "2.0");
                gen.writeNullProperty("id"); // Malformed transport input has no trustworthy request id.
                context.defaultSerializeProperty("error", safeError(error.getJsonRpcError()), gen);
                gen.writeEndObject();
            }
        });
        module.addSerializer(CallToolResult.class, new StdSerializer<CallToolResult>(CallToolResult.class) {
            @Override public void serialize(CallToolResult result, JsonGenerator gen, SerializationContext context) {
                boolean failed = Boolean.TRUE.equals(result.isError());
                gen.writeStartObject();
                gen.writeName("content");
                // Keep the declared content type so MCP's required `type` discriminator survives.
                var contentType = context.getTypeFactory().constructCollectionType(List.class, Content.class);
                context.findTypedValueSerializer(contentType, true).serialize(
                        failed ? List.of(new TextContent("Tool execution failed")) : result.content(), gen, context);
                if (result.isError() != null) context.defaultSerializeProperty("isError", result.isError(), gen);
                if (!failed && result.structuredContent() != null)
                    context.defaultSerializeProperty("structuredContent", result.structuredContent(), gen);
                if (!failed && result.meta() != null) context.defaultSerializeProperty("_meta", result.meta(), gen);
                gen.writeEndObject();
            }
        });
        return module;
    }
    @Bean(name = "mcpServerJsonMapper", defaultCandidate = false)
    JsonMapper mcpServerJsonMapper(SimpleModule safeMcpErrors) {
        return new McpServerJsonMapperAutoConfiguration().mcpServerJsonMapper().rebuild()
                .addModule(safeMcpErrors).build();
    }
    private static Map<String, Object> safeError(JSONRPCError error) {
        int code = error == null || error.code() == null ? -32603 : error.code();
        String message = switch (code) {
            case -32700 -> "Parse error";
            case -32600 -> "Invalid Request";
            case -32601 -> "Method not found";
            case -32602 -> "Invalid params";
            default -> "Internal error";
        };
        return Map.of("code", code, "message", message);
    }
}
