package com.safehome.integration;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.safehome.integration.building.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class CallHistoryTest {
    @TempDir Path directory;
    private static final String KEY = "synthetic-private-service-key";
    private static final String TOKEN = "synthetic-private-internal-token";
    private static final String PRIVATE = "synthetic-private-address-and-response";
    private static final BuildingQuery QUERY = new BuildingQuery("11530", "10700", "0", "0355", "0018",
            null, BuildingQuery.Ground.ABOVE_GROUND, 1, "101");
    private final JsonMapper mapper = JsonMapper.builder().build();
    private static final String EMPTY = "<response><header><resultCode>00</resultCode></header><body>"
            + "<pageNo>1</pageNo><totalCount>0</totalCount><items/></body></response>";

    @Test void recordsRetriesAndAllOperationsWithOneJobAndSurvivesReopening() throws Exception {
        var history = new FileCallHistory(directory);
        var calls = new AtomicInteger();
        var client = new BuildingHubClient(KEY, uri -> {
            if (calls.incrementAndGet() == 1) throw new UpstreamException("UPSTREAM_HTTP_503", true);
            return EMPTY.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }, history);
        var job = CallHistory.Job.create(42L, CallHistory.EntryPoint.REST);
        var result = new BuildingResolver(client, history).resolve(QUERY, job);
        assertEquals(Resolution.Status.NOT_FOUND, result.status());
        assertTrue(result.collectionComplete());
        var lines = lines();
        assertEquals(6, lines.size()); // start, failed Expos, retry, Title, Basis, finish
        for (String line : lines) {
            var event = mapper.readTree(line);
            assertEquals(job.id().toString(), event.path("job").path("id").asText());
            assertEquals(42, event.path("job").path("identificationId").asInt());
            assertTrue(event.path("elapsedMillis").asLong() >= 0);
            assertSafe(line);
        }
        assertEquals("UPSTREAM_HTTP_503", mapper.readTree(lines.get(1)).path("errorCode").asText());
        assertEquals(2, mapper.readTree(lines.get(2)).path("attempt").asInt());
        assertEquals("SUCCESS", mapper.readTree(lines.get(2)).path("outcome").asText());
        assertEquals("COMPLETED", mapper.readTree(lines.get(5)).path("outcome").asText());
        new FileCallHistory(directory).record(event(job, Instant.now()));
        assertEquals(7, lines().size());
        assertEquals(0, history.writeFailures());
    }
    @Test void malformedResponseAndAuthenticationAreNotRetriedOrRecordedVerbatim() throws Exception {
        var history = new FileCallHistory(directory);
        for (String response : List.of("<response>"+PRIVATE,
                "<OpenAPI_ServiceResponse><returnReasonCode>30</returnReasonCode><message>"+PRIVATE+"</message></OpenAPI_ServiceResponse>")) {
            var calls = new AtomicInteger();
            var client = new BuildingHubClient(KEY, uri -> {
                calls.incrementAndGet(); return response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }, history);
            var result = new BuildingResolver(client, history).resolve(QUERY);
            assertEquals(Resolution.Status.UNVERIFIABLE, result.status());
            assertFalse(result.collectionComplete());
            assertEquals(1, calls.get());
        }
        assertEquals(6, lines().size());
        for(String line: lines()) assertSafe(line);
        assertEquals("EXPOS_PAGE_1_MALFORMED_RESPONSE_XML", mapper.readTree(lines().get(2)).path("errorCode").asText());
        assertEquals("EXPOS_PAGE_1_UPSTREAM_REJECTED", mapper.readTree(lines().get(5)).path("errorCode").asText());
    }
    @Test void unknownExceptionsAndCodesCannotLeakIntoHistoryOrApplicationLogs() throws Exception {
        var history = new FileCallHistory(directory);
        var appender = capture();
        try {
            for(boolean typed:List.of(true,false)) {
                var client = new BuildingHubClient(KEY, uri -> {
                    if(typed) throw new UpstreamException(PRIVATE+"\n"+TOKEN+uri, false);
                    throw new IllegalStateException(PRIVATE+"\n"+TOKEN+uri);
                }, history);
                var result = new BuildingResolver(client, history).resolve(QUERY);
                assertEquals(Resolution.Status.UNVERIFIABLE, result.status());
                assertFalse(result.collectionComplete());
            }
            for(String line:lines()) assertSafe(line);
            assertTrue(lines().stream().anyMatch(line -> line.contains("UNEXPECTED_FAILURE")));
            for(var log:appender.list) { assertSafe(log.getFormattedMessage()); assertNull(log.getThrowableProxy()); }
        } finally { detach(appender); }
    }
    @Test void incompleteSecondPageIsRecordedWithoutConfirmingPartialData() throws Exception {
        var history = new FileCallHistory(directory);
        var client = new BuildingHubClient(KEY, uri -> (uri.getQuery().contains("pageNo=1&")
                ? EMPTY.replace("<totalCount>0</totalCount>","<totalCount>2</totalCount>")
                    .replace("<items/>","<items><item><mgmBldrgstPk>synthetic-unit</mgmBldrgstPk></item></items>")
                : "<response>"+PRIVATE).getBytes(java.nio.charset.StandardCharsets.UTF_8),history);
        var result=new BuildingResolver(client,history).resolve(QUERY);
        assertEquals(Resolution.Status.UNVERIFIABLE,result.status());
        assertFalse(result.collectionComplete()); assertTrue(result.candidates().isEmpty());
        var events=lines(); assertEquals(4,events.size());
        assertEquals(2,mapper.readTree(events.get(2)).path("page").asInt());
        assertEquals("FAILURE",mapper.readTree(events.get(2)).path("outcome").asText());
        assertEquals("EXPOS_PAGE_2_MALFORMED_RESPONSE_XML",mapper.readTree(events.get(3)).path("errorCode").asText());
        for(var line:events)assertSafe(line);
    }
    @Test void writeFailureIsVisibleWithoutChangingResultOrLoggingPrivateException() throws Exception {
        Path target=directory.resolve("history");
        var history = new FileCallHistory(target);
        Files.delete(target); Files.writeString(target, PRIVATE);
        var appender=capture();
        try {
            var result=new BuildingResolver(new BuildingHubClient(KEY, uri -> EMPTY.getBytes(), history),history).resolve(QUERY);
            assertEquals(Resolution.Status.NOT_FOUND,result.status());
            assertTrue(result.collectionComplete());
            assertEquals(5,history.writeFailures());
            assertEquals(5,appender.list.size());
            for(var log:appender.list) {
                assertEquals("BUILDING_CALL_HISTORY_WRITE_FAILED",log.getFormattedMessage());
                assertNull(log.getThrowableProxy()); assertSafe(log.getFormattedMessage());
            }
        } finally {detach(appender);}
    }
    @Test void concurrentWritesRemainValidAndUtcDatesRotateFiles() throws Exception {
        var history=new FileCallHistory(directory);
        var job=CallHistory.Job.create(null,CallHistory.EntryPoint.MCP);
        Instant before=Instant.parse("2026-10-20T23:59:59Z"),after=before.plusSeconds(2);
        IntStream.range(0,40).parallel().forEach(n->history.record(event(job,n%2==0?before:after)));
        assertEquals(40,lines().size());
        try(var files=Files.list(directory)){assertEquals(2,files.count());}
        for(var line:lines()) assertEquals("MCP",mapper.readTree(line).path("job").path("entryPoint").asText());
        assertEquals(0,history.writeFailures());
    }
    private CallHistory.Event event(CallHistory.Job job,Instant time) {
        return new CallHistory.Event(time,job,CallHistory.Kind.JOB_STARTED,null,null,null,CallHistory.Outcome.STARTED,0,null,null,null);
    }
    private List<String> lines() throws Exception {
        try(var files=Files.list(directory)) {
            return files.filter(p->p.toString().endsWith(".jsonl")).sorted().flatMap(p->{
                try{return Files.readAllLines(p).stream();}catch(Exception e){throw new IllegalStateException(e);}
            }).toList();
        }
    }
    private static ListAppender<ILoggingEvent> capture() {
        var appender=new ListAppender<ILoggingEvent>(); appender.start();
        ((Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).addAppender(appender); return appender;
    }
    private static void detach(ListAppender<ILoggingEvent> appender) {
        ((Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).detachAppender(appender); appender.stop();
    }
    private static void assertSafe(String text) {
        for(String forbidden:List.of(KEY,TOKEN,PRIVATE,"serviceKey=","https://","stackTrace","Authorization","X-SafeHome-Integration-Token"))
            assertFalse(text.contains(forbidden),"Private data found in diagnostic output");
    }
}
