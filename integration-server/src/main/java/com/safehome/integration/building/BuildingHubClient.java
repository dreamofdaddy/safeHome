package com.safehome.integration.building;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.helpers.DefaultHandler;

public final class BuildingHubClient {
    static final int MAX_RESPONSE_BYTES = 2_000_000;
    public enum Operation {
        TITLE("getBrTitleInfo"), EXPOS("getBrExposInfo"), BASIS("getBrBasisOulnInfo");
        final String path;
        Operation(String path) { this.path = path; }
    }
    @FunctionalInterface public interface Transport { byte[] get(URI uri); }
    private static final String BASE = "https://apis.data.go.kr/1613000/BldRgstHubService/";
    private final String serviceKey;
    private final Transport transport;
    private final CallHistory.Sink history;
    private long lastCall;
    public BuildingHubClient(String serviceKey) {
        this(serviceKey, defaultTransport(), CallHistory.NONE);
    }
    public BuildingHubClient(String serviceKey, Transport transport) {
        this(serviceKey, transport, CallHistory.NONE);
    }
    public BuildingHubClient(String serviceKey, Transport transport, CallHistory.Sink history) {
        this.serviceKey = serviceKey;
        this.transport = transport;
        this.history = history;
    }
    public static BuildingHubClient recorded(String serviceKey, CallHistory.Sink history) {
        return new BuildingHubClient(serviceKey, defaultTransport(), history);
    }
    static Transport defaultTransport() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        return uri -> {
            try {
                HttpResponse<byte[]> response = client.send(HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(8)).GET().build(), info -> new LimitedBodySubscriber(
                                MAX_RESPONSE_BYTES, info.headers().firstValueAsLong("Content-Length").orElse(-1)));
                int status = response.statusCode();
                if (status != 200) throw new UpstreamException("UPSTREAM_HTTP_" + status,
                        status == 502 || status == 503 || status == 504);
                return response.body();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new UpstreamException("INTERRUPTED", false);
            } catch (java.io.IOException e) {
                for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                    if (cause instanceof LimitedBodySubscriber.ResponseTooLargeException)
                        throw new UpstreamException("RESPONSE_SIZE", false);
                }
                throw new UpstreamException("UPSTREAM_IO", true);
            }
        };
    }
    public List<BuildingUnit> fetchAll(Operation operation, BuildingQuery query, long deadlineNanos) {
        return fetchAll(operation, query, deadlineNanos, CallHistory.Job.create(null, CallHistory.EntryPoint.DIRECT));
    }
    public List<BuildingUnit> fetchAll(Operation operation, BuildingQuery query, long deadlineNanos, CallHistory.Job job) {
        return new AllPages().collect(page -> fetchPage(operation, query, page, deadlineNanos, job), 200, 20000);
    }
    private AllPages.Page fetchPage(Operation operation, BuildingQuery q, int page, long deadline, CallHistory.Job job) {
        if (serviceKey == null || serviceKey.isBlank()) throw new UpstreamException("SERVICE_KEY_NOT_CONFIGURED", false);
        String params = "serviceKey=" + URLEncoder.encode(serviceKey, StandardCharsets.UTF_8)
                + "&sigunguCd=" + q.sigunguCd() + "&bjdongCd=" + q.bjdongCd()
                + "&platGbCd=" + q.platGbCd() + "&bun=" + q.bun() + "&ji=" + q.ji()
                + "&numOfRows=100&pageNo=" + page + "&_type=xml";
        URI uri = URI.create(BASE + operation.path + "?" + params);
        for (int attempt = 0; ; attempt++) {
            if (Thread.currentThread().isInterrupted()) throw new UpstreamException("INTERRUPTED", false);
            if (System.nanoTime() + Duration.ofSeconds(9).toNanos() >= deadline)
                throw new UpstreamException("COLLECTION_DEADLINE", false);
            pace();
            long started = System.nanoTime();
            try {
                AllPages.Page result = parse(transport.get(uri));
                record(job, operation, page, attempt + 1, started, null, null);
                return result;
            } catch (UpstreamException e) {
                String code = CallHistory.safeCode(e.getMessage());
                record(job, operation, page, attempt + 1, started, code, e.xmlFailure());
                if (!e.retryable() || attempt >= 3) {
                    // Only fixed operation names, page numbers and safe codes are retained.
                    throw new UpstreamException(operation.name() + "_PAGE_" + page + "_" + code,
                            e.retryable(), e.xmlFailure());
                }
                pause(300L * (attempt + 1));
            } catch (RuntimeException ignored) {
                record(job, operation, page, attempt + 1, started, "UNEXPECTED_FAILURE", null);
                throw new UpstreamException("UNEXPECTED_FAILURE", false);
            }
        }
    }
    private void record(CallHistory.Job job, Operation operation, int page, int attempt, long start, String code, XmlFailure xmlFailure) {
        CallHistory.emit(history, new CallHistory.Event(Instant.now(), job, CallHistory.Kind.API_ATTEMPT,
                operation, page, attempt, code == null ? CallHistory.Outcome.SUCCESS : CallHistory.Outcome.FAILURE,
                CallHistory.elapsed(start), code, null, null, xmlFailure));
    }
    // Global spacing per client: at most 5 upstream calls/second, retries included.
    private synchronized void pace() {
        long wait = 200 - (System.nanoTime() - lastCall) / 1_000_000;
        if (wait > 0) pause(wait);
        lastCall = System.nanoTime();
    }
    private static void pause(long millis) {
        try { Thread.sleep(millis); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamException("INTERRUPTED", false);
        }
    }
    static AllPages.Page parse(byte[] bytes) {
        String stage = "XML_SETUP";
        try {
            if (bytes == null || bytes.length > MAX_RESPONSE_BYTES) throw new UpstreamException("RESPONSE_SIZE", false);
            if (bytes.length == 0) throw new UpstreamException("EMPTY_RESPONSE", true);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler());
            stage = "XML";
            Element root = builder.parse(new ByteArrayInputStream(bytes)).getDocumentElement();
            stage = "HEADER";
            String code = value(root, "resultCode");
            if (code == null) code = value(root, "returnReasonCode");
            if (!"00".equals(code) && !"0".equals(code)) {
                boolean retry = "01".equals(code) || "05".equals(code) || "23".equals(code);
                throw new UpstreamException("UPSTREAM_REJECTED", retry);
            }
            stage = "PAGINATION";
            int page = Integer.parseInt(value(root, "pageNo"));
            int total = Integer.parseInt(value(root, "totalCount"));
            stage = "ITEMS";
            NodeList items = root.getElementsByTagName("item");
            List<BuildingUnit> units = new ArrayList<>();
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String groundCode = value(item, "flrGbCd");
                BuildingQuery.Ground ground = "10".equals(groundCode) ? BuildingQuery.Ground.BELOW_GROUND
                        : "20".equals(groundCode) ? BuildingQuery.Ground.ABOVE_GROUND : null;
                Integer floor = null;
                try {
                    String raw = value(item, "flrNo");
                    if (raw != null) floor = new java.math.BigDecimal(raw).intValueExact();
                    if (floor != null && floor <= 0) floor = null;
                } catch (ArithmeticException | NumberFormatException ignored) { }
                units.add(new BuildingUnit(value(item, "mgmBldrgstPk"), value(item, "mgmUpBldrgstPk"),
                        value(item, "sigunguCd"), value(item, "bjdongCd"), value(item, "platGbCd"),
                        value(item, "bun"), value(item, "ji"), value(item, "platPlc"),
                        value(item, "newPlatPlc"), value(item, "dongNm"), ground, floor,
                        value(item, "hoNm"), value(item, "mainPurpsCdNm")));
            }
            return new AllPages.Page(page, total, units);
        } catch (UpstreamException e) { throw e; }
        catch (org.xml.sax.SAXParseException e) {
            XmlFailure diagnostic = XmlFailure.from(bytes, e);
            throw new UpstreamException("MALFORMED_RESPONSE_XML",
                    diagnostic.kind() != XmlFailure.Kind.DOCTYPE_FORBIDDEN, diagnostic);
        }
        catch (Exception e) { throw new UpstreamException("MALFORMED_RESPONSE_" + stage, false); }
    }
    private static String value(Element element, String tag) {
        NodeList nodes = element.getElementsByTagName(tag);
        if (nodes.getLength() != 1) return null;
        String value = nodes.item(0).getTextContent().strip();
        return value.isEmpty() ? null : value;
    }
}
