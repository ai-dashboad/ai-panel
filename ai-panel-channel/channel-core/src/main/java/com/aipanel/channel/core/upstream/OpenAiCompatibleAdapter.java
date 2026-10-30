package com.aipanel.channel.core.upstream;

import com.aipanel.channel.api.dto.request.ChatCompletionRequest;
import com.aipanel.channel.api.dto.response.ChatCompletionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Adapter for any server that implements the OpenAI chat completions API:
 * vLLM, Ollama ({@code /v1}), LM Studio, SGLang, DeepSeek, Moonshot, Zhipu, Qwen (compatible mode), ...
 * <p>
 * The client's request is forwarded as-is (only fields the client sent; {@code tools},
 * {@code tool_choice}, {@code parallel_tool_calls}, tool messages, image content parts and unknown
 * extra fields are preserved). Streaming chunks are forwarded verbatim.
 */
@Slf4j
@Component
public class OpenAiCompatibleAdapter implements ChatAdapter {

    protected final UpstreamHttp http;

    public OpenAiCompatibleAdapter() {
        this(new UpstreamHttp());
    }

    public OpenAiCompatibleAdapter(UpstreamHttp http) {
        this.http = http;
    }

    @Override
    public boolean supports(String providerType) {
        String t = ProviderTypes.normalize(providerType);
        return ProviderTypes.isSupported(t) && !ProviderTypes.isAnthropic(t) && !ProviderTypes.OPENAI.equals(t);
    }

    /** Whether an API key is mandatory for this provider. */
    protected boolean keyRequired(UpstreamTarget target) {
        return !ProviderTypes.isKeyOptional(target.provider().type());
    }

    protected Map<String, String> headers(UpstreamTarget target, boolean stream) {
        if (keyRequired(target) && (target.apiKey() == null || target.apiKey().isBlank())) {
            throw new UpstreamException(503, "no_api_key",
                "No API key configured for provider '" + target.provider().name() + "'", true);
        }
        Map<String, String> h = new LinkedHashMap<>();
        h.put("Content-Type", "application/json");
        h.put("Accept", stream ? "text/event-stream" : "application/json");
        if (target.apiKey() != null && !target.apiKey().isBlank()) {
            h.put("Authorization", "Bearer " + target.apiKey());
        }
        return h;
    }

    /**
     * Build the upstream JSON body: exactly the fields the client sent (no forced defaults).
     */
    public ObjectNode buildBody(ChatCompletionRequest request, boolean stream) {
        ObjectNode body = UpstreamHttp.MAPPER.valueToTree(request);
        body.remove("stream");
        if (stream) {
            body.put("stream", true);
        } else {
            body.remove("stream_options");
        }
        return body;
    }

    @Override
    public ChatCompletionResponse complete(UpstreamTarget target, ChatCompletionRequest request) {
        UpstreamProvider p = target.provider();
        String url = BaseUrls.chatCompletionsUrl(p.baseUrl(), p.type());
        HttpResponse<String> resp = http.postJson(url, headers(target, false),
            buildBody(request, false).toString(), p.timeoutSeconds());
        UpstreamHttp.ensureOk(resp);
        return parseResponse(resp.body());
    }

    static ChatCompletionResponse parseResponse(String body) {
        try {
            ChatCompletionResponse r = UpstreamHttp.MAPPER.readValue(body, ChatCompletionResponse.class);
            if (r.getChoices() == null) {
                throw new UpstreamException(502, "upstream_invalid_response",
                    "Upstream returned a response without choices: " + UpstreamHttp.errorMessage(body), true);
            }
            if (r.getObject() == null) {
                r.setObject("chat.completion");
            }
            return r;
        } catch (JsonProcessingException e) {
            throw new UpstreamException(502, "upstream_invalid_response",
                "Upstream returned invalid JSON: " + UpstreamHttp.errorMessage(body), true, e);
        }
    }

    @Override
    public StreamResult stream(UpstreamTarget target, ChatCompletionRequest request, StreamSink sink) {
        UpstreamProvider p = target.provider();
        String url = BaseUrls.chatCompletionsUrl(p.baseUrl(), p.type());
        HttpResponse<Stream<String>> resp = http.postStream(url, headers(target, true),
            buildBody(request, true).toString(), p.timeoutSeconds());
        UpstreamHttp.ensureStreamOk(resp);

        StreamResult result = new StreamResult();
        StringBuilder nonSse = new StringBuilder();
        boolean sawData = false;
        try (Stream<String> lines = resp.body()) {
            Iterator<String> it = lines.iterator();
            while (it.hasNext()) {
                String line = it.next();
                if (line.startsWith("data:")) {
                    sawData = true;
                    String payload = line.substring(5).trim();
                    if (payload.isEmpty()) {
                        continue;
                    }
                    if ("[DONE]".equals(payload)) {
                        break;
                    }
                    track(payload, result);
                    emit(sink, payload);
                    result.setChunks(result.getChunks() + 1);
                } else if (!sawData && !line.isBlank() && !line.startsWith(":") && !line.startsWith("event:")
                        && !line.startsWith("id:") && !line.startsWith("retry:")) {
                    nonSse.append(line).append('\n');
                }
            }
        } catch (java.io.UncheckedIOException e) {
            if (result.getChunks() == 0) {
                throw new UpstreamException(502, "upstream_stream_error", "Upstream stream failed: " + e.getMessage(), true, e);
            }
            throw new UpstreamException(502, "upstream_stream_error", "Upstream stream interrupted: " + e.getMessage(), false, e);
        }

        if (!sawData && nonSse.length() > 0) {
            // Upstream ignored stream=true and answered with plain JSON: convert it.
            ChatCompletionResponse full = parseResponse(nonSse.toString());
            boolean includeUsage = request.getStreamOptions() != null
                && Boolean.TRUE.equals(request.getStreamOptions().get("include_usage"));
            for (String chunk : OpenAiChunks.fromResponse(full, includeUsage)) {
                emit(sink, chunk);
                result.setChunks(result.getChunks() + 1);
            }
            result.setModel(full.getModel());
            result.setUsage(full.getUsage());
            if (!full.getChoices().isEmpty()) {
                result.setFinishReason(full.getChoices().get(0).getFinishReason());
                ChatCompletionResponse.Message m = full.getChoices().get(0).getMessage();
                if (m != null && m.getContent() != null) {
                    result.getGeneratedText().append(m.getContent());
                }
            }
        }
        return result;
    }

    protected static void emit(StreamSink sink, String payload) {
        try {
            sink.send(payload);
        } catch (IOException e) {
            throw new ClientDisconnectedException(e);
        }
    }

    /** Record model / usage / finish_reason / generated text from a forwarded chunk. */
    static void track(String payload, StreamResult result) {
        try {
            JsonNode node = UpstreamHttp.MAPPER.readTree(payload);
            if (node.path("model").isTextual()) {
                result.setModel(node.get("model").asText());
            }
            JsonNode usage = node.get("usage");
            if (usage != null && usage.isObject()) {
                result.setUsage(UpstreamHttp.MAPPER.treeToValue(usage, ChatCompletionResponse.Usage.class));
            }
            for (JsonNode choice : node.path("choices")) {
                JsonNode fr = choice.get("finish_reason");
                if (fr != null && fr.isTextual()) {
                    result.setFinishReason(fr.asText());
                }
                JsonNode delta = choice.path("delta");
                if (delta.path("content").isTextual()) {
                    result.getGeneratedText().append(delta.get("content").asText());
                }
                if (delta.path("reasoning_content").isTextual()) {
                    result.getGeneratedText().append(delta.get("reasoning_content").asText());
                }
                for (JsonNode tc : delta.path("tool_calls")) {
                    JsonNode fn = tc.path("function");
                    if (fn.path("name").isTextual()) {
                        result.getGeneratedText().append(fn.get("name").asText());
                    }
                    if (fn.path("arguments").isTextual()) {
                        result.getGeneratedText().append(fn.get("arguments").asText());
                    }
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            log.debug("Could not inspect stream chunk: {}", e.getMessage());
        }
    }

    @Override
    public List<String> listModels(UpstreamTarget target) {
        UpstreamProvider p = target.provider();
        Map<String, String> h = new LinkedHashMap<>();
        h.put("Accept", "application/json");
        if (target.apiKey() != null && !target.apiKey().isBlank()) {
            h.put("Authorization", "Bearer " + target.apiKey());
        }
        HttpResponse<String> resp = http.get(BaseUrls.modelsUrl(p.baseUrl(), p.type()), h,
            Math.min(p.timeoutSeconds() > 0 ? p.timeoutSeconds() : 30, 30));
        UpstreamHttp.ensureOk(resp);
        List<String> ids = new ArrayList<>();
        try {
            for (JsonNode m : UpstreamHttp.MAPPER.readTree(resp.body()).path("data")) {
                if (m.path("id").isTextual()) {
                    ids.add(m.get("id").asText());
                }
            }
        } catch (IOException e) {
            throw new UpstreamException(502, "upstream_invalid_response", "Invalid /models response", false, e);
        }
        return ids;
    }
}
