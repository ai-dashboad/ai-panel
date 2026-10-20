package com.aipanel.channel.core.upstream;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tiny mock upstream (JDK HttpServer) for adapter / router tests.
 * Responses are registered per path; every request is recorded.
 */
public class MockUpstream implements AutoCloseable {

    public record Recorded(String method, String path, Map<String, List<String>> headers, String body) {
        public JsonNode json() {
            try {
                return UpstreamHttp.MAPPER.readTree(body);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        public String header(String name) {
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                if (e.getKey().equalsIgnoreCase(name)) {
                    return e.getValue().get(0);
                }
            }
            return null;
        }
    }

    private record Response(int status, String contentType, List<String> parts) {
    }

    private final HttpServer server;
    private final Map<String, Response> responses = new ConcurrentHashMap<>();
    public final List<Recorded> requests = Collections.synchronizedList(new ArrayList<>());

    public MockUpstream() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public MockUpstream json(String path, int status, String body) {
        responses.put(path, new Response(status, "application/json", List.of(body)));
        return this;
    }

    /** SSE response: each event is written and flushed separately. */
    public MockUpstream sse(String path, List<String> events) {
        responses.put(path, new Response(200, "text/event-stream", events));
        return this;
    }

    public Recorded last() {
        return requests.get(requests.size() - 1);
    }

    private void handle(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        requests.add(new Recorded(ex.getRequestMethod(), ex.getRequestURI().getPath(),
            Map.copyOf(ex.getRequestHeaders()), body));
        Response r = responses.get(ex.getRequestURI().getPath());
        if (r == null) {
            byte[] nf = "{\"error\":{\"message\":\"not found\"}}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(404, nf.length);
            ex.getResponseBody().write(nf);
            ex.close();
            return;
        }
        ex.getResponseHeaders().add("Content-Type", r.contentType());
        ex.sendResponseHeaders(r.status(), 0);
        try (OutputStream out = ex.getResponseBody()) {
            for (String part : r.parts()) {
                out.write(part.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
