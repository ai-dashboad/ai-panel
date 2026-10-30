package com.aipanel.channel.core.upstream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Minimal JSON/SSE HTTP client shared by the upstream adapters (JDK HttpClient, HTTP/1.1).
 */
public class UpstreamHttp {

    public static final ObjectMapper MAPPER = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();

    private final HttpClient client;

    public UpstreamHttp() {
        this(HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build());
    }

    public UpstreamHttp(HttpClient client) {
        this.client = client;
    }

    public HttpResponse<String> postJson(String url, Map<String, String> headers, String body, int timeoutSeconds) {
        return send(request(url, headers, timeoutSeconds).POST(HttpRequest.BodyPublishers.ofString(body)).build(),
            HttpResponse.BodyHandlers.ofString(), url);
    }

    public HttpResponse<Stream<String>> postStream(String url, Map<String, String> headers, String body, int timeoutSeconds) {
        return send(request(url, headers, timeoutSeconds).POST(HttpRequest.BodyPublishers.ofString(body)).build(),
            HttpResponse.BodyHandlers.ofLines(), url);
    }

    public HttpResponse<String> get(String url, Map<String, String> headers, int timeoutSeconds) {
        return send(request(url, headers, timeoutSeconds).GET().build(), HttpResponse.BodyHandlers.ofString(), url);
    }

    private HttpRequest.Builder request(String url, Map<String, String> headers, int timeoutSeconds) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(timeoutSeconds > 0 ? timeoutSeconds : 60));
        headers.forEach(b::header);
        return b;
    }

    private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler, String url) {
        try {
            return client.send(request, handler);
        } catch (HttpTimeoutException e) {
            throw new UpstreamException(504, "upstream_timeout", "Upstream timed out: " + safeUrl(url), true, e);
        } catch (IOException e) {
            throw new UpstreamException(502, "upstream_unreachable",
                "Cannot reach upstream " + safeUrl(url) + ": " + e.getMessage(), true, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamException(502, "upstream_interrupted", "Interrupted while calling upstream", false, e);
        }
    }

    /** Throw a mapped {@link UpstreamException} for HTTP error responses. */
    public static void ensureOk(HttpResponse<String> response) {
        if (response.statusCode() >= 400) {
            throw UpstreamException.fromUpstreamStatus(response.statusCode(), errorMessage(response.body()));
        }
    }

    /** Same as {@link #ensureOk(HttpResponse)} for streaming responses; drains the error body. */
    public static void ensureStreamOk(HttpResponse<Stream<String>> response) {
        if (response.statusCode() >= 400) {
            String body;
            try (Stream<String> lines = response.body()) {
                body = lines.limit(200).collect(Collectors.joining("\n"));
            } catch (RuntimeException e) {
                body = "";
            }
            throw UpstreamException.fromUpstreamStatus(response.statusCode(), errorMessage(body));
        }
    }

    /** Extract a human readable message from an upstream error body. */
    public static String errorMessage(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonNode node = MAPPER.readTree(body);
            JsonNode err = node.path("error");
            if (err.isTextual()) {
                return err.asText();
            }
            if (err.path("message").isTextual()) {
                return err.path("message").asText();
            }
            if (node.path("message").isTextual()) {
                return node.path("message").asText();
            }
            if (node.path("detail").isTextual()) {
                return node.path("detail").asText();
            }
        } catch (IOException ignored) {
            // not JSON
        }
        String trimmed = body.strip();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) + "..." : trimmed;
    }

    /** URL without query string (never log credentials passed as query params). */
    static String safeUrl(String url) {
        int q = url.indexOf('?');
        return q >= 0 ? url.substring(0, q) : url;
    }
}
