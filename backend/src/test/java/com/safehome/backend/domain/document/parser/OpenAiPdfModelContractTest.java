package com.safehome.backend.domain.document.parser;

import com.safehome.backend.domain.buildingregister.OpenAiBuildingRegisterParser;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

class OpenAiPdfModelContractTest {
    private final JsonMapper mapper=JsonMapper.builder().build();
    private JsonNode call(boolean building, String model, boolean incomplete) throws Exception {
        var captured=new AtomicReference<JsonNode>();var calls=new AtomicInteger();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        String extracted=building ? "{\"buildingRegisterDocument\":false,\"entries\":[]}"
            : mapper.writeValueAsString(new ParsedRegistryDocument(null,null,null,null,null,null,List.of(),List.of()));
        String response=incomplete ? "{\"status\":\"incomplete\",\"incomplete_details\":{\"reason\":\"max_output_tokens\"},\"output\":[]}"
            : mapper.writeValueAsString(Map.of("status","completed","output",List.of(
                Map.of("type","reasoning","summary",List.of()),
                Map.of("type","message","content",List.of(Map.of("type","output_text","text",extracted))))));
        server.createContext("/responses",exchange->{
            try {
                calls.incrementAndGet();captured.set(mapper.readTree(exchange.getRequestBody().readAllBytes()));
                byte[] bytes=response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,bytes.length);
                exchange.getResponseBody().write(bytes);
            } finally {exchange.close();}
        });
        server.start();
        try {
            String endpoint="http://127.0.0.1:"+server.getAddress().getPort()+"/responses";
            Runnable action=building
                ? ()->new OpenAiBuildingRegisterParser(mapper,"fake-test-key",model,endpoint).parse(List.of(new byte[]{1,2}))
                : ()->new OpenAiRegistryDocumentParser(mapper,"fake-test-key",model,endpoint).parse(new byte[]{1,2});
            if(incomplete)assertThatThrownBy(action::run).isInstanceOf(RegistryParseException.class).hasMessage("PARSER_INCOMPLETE");
            else assertThatCode(action::run).doesNotThrowAnyException();
            assertThat(calls.get()).isEqualTo(1);
            return captured.get();
        } finally {server.stop(0);}
    }
    @Test void bothParsersUseSupportedSolReasoningAndKeepHighDetailPrivateStructuredInputs() throws Exception {
        for(boolean building:new boolean[]{false,true}) {
            var request=call(building,"gpt-6.1-sol",false);
            assertThat(request.path("model").asString()).isEqualTo("gpt-6.1-sol");
            assertThat(request.path("reasoning").path("effort").asString()).isEqualTo("low");
            assertThat(request.path("max_output_tokens").asInt()).isEqualTo(25000);
            assertThat(request.path("store").asBoolean()).isFalse();
            assertThat(request.path("text").path("format").path("strict").asBoolean()).isTrue();
            var content=request.path("input").get(0).path("content");
            boolean foundFile=false;
            for(var part:content)if("input_file".equals(part.path("type").asString())) {
                foundFile=true;assertThat(part.path("detail").asString()).isEqualTo("high");
            }
            assertThat(foundFile).isTrue();
        }
    }
    @Test void legacyModelRollbackKeepsItsOriginalRequestContract() throws Exception {
        for(boolean building:new boolean[]{false,true}) {
            var request=call(building,"gpt-4.1",false);
            assertThat(request.has("reasoning")).isFalse();
            assertThat(request.path("max_output_tokens").asInt()).isEqualTo(building?8000:3000);
        }
    }
    @Test void incompleteReasoningResponsesAreRejectedWithoutAutomaticRetries() throws Exception {
        call(false,"gpt-6.1-sol",true);call(true,"gpt-6.1-sol",true);
    }
    @Test
    @org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
    void providerErrorsLogHttpStatusWithoutResponseBodyAndNeverRetry(
            org.springframework.boot.test.system.CapturedOutput output) throws Exception {
        var calls=new AtomicInteger();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/responses",exchange->{
            try {
                calls.incrementAndGet();exchange.getRequestBody().readAllBytes();
                byte[] bytes="PRIVATE_PROVIDER_BODY_SENTINEL".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(400,bytes.length);exchange.getResponseBody().write(bytes);
            } finally {exchange.close();}
        });
        server.start();
        try {
            var parser=new OpenAiRegistryDocumentParser(mapper,"fake-test-key","gpt-6.1-sol",
                "http://127.0.0.1:"+server.getAddress().getPort()+"/responses");
            assertThatThrownBy(()->parser.parse(new byte[]{1,2})).hasMessage("PARSER_PROVIDER_ERROR");
            assertThat(calls.get()).isEqualTo(1);
            assertThat(output.getAll()).contains("status=400").doesNotContain("PRIVATE_PROVIDER_BODY_SENTINEL","fake-test-key");
        } finally {server.stop(0);}
    }

}
