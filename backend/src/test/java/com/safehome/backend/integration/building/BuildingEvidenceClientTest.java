package com.safehome.backend.integration.building;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BuildingEvidenceClientTest {
    @Test void sendsOnlyOptionalHistoryIdAndPreservesResolutionContract() throws Exception {
        var ids=new ArrayList<String>();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/internal/v1/buildings/resolve",exchange->{
            ids.add(exchange.getRequestHeaders().getFirst("X-SafeHome-Identification-Id"));
            exchange.getRequestBody().readAllBytes();
            byte[] body="""
                    {"success":true,"data":{"status":"UNVERIFIABLE","candidates":[],
                    "reason":"SERVICE_KEY_NOT_CONFIGURED","source":"MOLIT_BUILDING_HUB",
                    "queriedAt":"2026-10-21T00:00:00Z","collectionComplete":false}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(200,body.length);
            try(var out=exchange.getResponseBody()){out.write(body);}
        });
        server.start();
        try {
            var client=new BuildingEvidenceClient("http://127.0.0.1:"+server.getAddress().getPort(),
                    "synthetic-only-internal-token-32-characters");
            var query=new BuildingQuery("11530","10700","0","0355","0018",null,
                    BuildingQuery.Ground.ABOVE_GROUND,1,"101");
            var result=client.resolve(query,42L);
            assertEquals(Resolution.Status.UNVERIFIABLE,result.status());
            assertFalse(result.collectionComplete());
            assertEquals("SERVICE_KEY_NOT_CONFIGURED",result.reason());
            client.resolve(query);
            assertEquals("42",ids.get(0)); assertNull(ids.get(1));
        } finally {server.stop(0);}
    }
}
