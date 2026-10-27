package com.safehome.backend.domain.buildingregister;

import static org.assertj.core.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import com.safehome.backend.domain.document.parser.RegistryParseException;

class BuildingRegisterParserTest {
    @Test void sendsTheSyntheticBundleOnceWithStructuredOutputAndNoProviderStorage() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var calls=new AtomicInteger();
        var mapper=JsonMapper.builder().build();
        server.createContext("/responses",exchange->{
            calls.incrementAndGet();var request=mapper.readTree(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            assertThat(request.path("store").asBoolean()).isFalse();
            assertThat(request.path("text").path("format").path("strict").asBoolean()).isTrue();
            var content=request.path("input").get(0).path("content");int fileCount=0;String text="";
            for(var item:content) {if("input_file".equals(item.path("type").asString())) fileCount++;else text+=item.path("text").asString();}
            assertThat(fileCount).isEqualTo(2);assertThat(text).contains("ORIGINAL fileIndex=1 of 2","ORIGINAL fileIndex=2 of 2","EACH original fileIndex");
            String result=mapper.writeValueAsString(java.util.Map.of("status","completed","output",List.of(java.util.Map.of("content",List.of(java.util.Map.of("type","output_text","text","{\"buildingRegisterDocument\":false,\"entries\":[]}"))))));
            byte[] bytes=result.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,bytes.length);
            try(var output=exchange.getResponseBody()){output.write(bytes);}
        });server.start();
        try {
            var parser=new OpenAiBuildingRegisterParser(mapper,"synthetic-key","gpt-4.1","http://127.0.0.1:"+server.getAddress().getPort()+"/responses");
            assertThat(parser.parse(List.of("%PDF-synthetic-1".getBytes(),"%PDF-synthetic-2".getBytes())).buildingRegisterDocument()).isFalse();
            assertThat(calls.get()).isEqualTo(1);
        } finally {server.stop(0);}
    }
    @Test void missingConfigurationNeverAttemptsExternalParsing() {
        var parser=new OpenAiBuildingRegisterParser(JsonMapper.builder().build(),"","gpt-4.1","https://api.openai.com/v1/responses");
        assertThatThrownBy(()->parser.parse(List.of(new byte[0]))).isInstanceOf(RegistryParseException.class).hasMessage("PARSER_NOT_CONFIGURED");
    }
}
