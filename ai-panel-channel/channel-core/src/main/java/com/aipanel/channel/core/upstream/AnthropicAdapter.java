package com.aipanel.channel.core.upstream;

import com.aipanel.channel.api.dto.request.ChatCompletionRequest;
import com.aipanel.channel.api.dto.response.ChatCompletionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Adapter for the Anthropic Messages API. Translates OpenAI chat completions requests
 * (including tools / tool_choice / tool messages / image parts) to Anthropic and translates
 * responses and stream events (tool_use, input_json_delta) back to OpenAI format.
 */
@Slf4j
@Component
public class AnthropicAdapter implements ChatAdapter {

    public static final String API_VERSION = "2023-06-01";

    /**
     * Anthropic requires {@code max_tokens}; this value is only used when the client sent neither
     * {@code max_tokens} nor {@code max_completion_tokens}.
     */
    public static final int DEFAULT_MAX_TOKENS = 4096;

    private final UpstreamHttp http;

    public AnthropicAdapter() {
        this(new UpstreamHttp());
    }

    public AnthropicAdapter(UpstreamHttp http) {
        this.http = http;
    }

    @Override
    public boolean supports(String providerType) {
        return ProviderTypes.isAnthropic(providerType);
    }

    private Map<String, String> headers(UpstreamTarget target, boolean stream) {
        if (target.apiKey() == null || target.apiKey().isBlank()) {
            throw new UpstreamException(503, "no_api_key",
                "No API key configured for provider '" + target.provider().name() + "'", true);
        }
        Map<String, String> h = new LinkedHashMap<>();
        h.put("Content-Type", "application/json");
        h.put("Accept", stream ? "text/event-stream" : "application/json");
        h.put("x-api-key", target.apiKey());
        h.put("anthropic-version", API_VERSION);
        return h;
    }

    // ------------------------------------------------------------------ request mapping

    public ObjectNode buildBody(ChatCompletionRequest req, boolean stream) {
        ObjectNode body = UpstreamHttp.MAPPER.createObjectNode();
        body.put("model", req.getModel());

        Integer maxTokens = req.getMaxTokens();
        Object mct = req.getExtra().get("max_completion_tokens");
        if (maxTokens == null && mct instanceof Number n) {
            maxTokens = n.intValue();
        }
        body.put("max_tokens", maxTokens != null ? maxTokens : DEFAULT_MAX_TOKENS);

        StringBuilder system = new StringBuilder();
        ArrayNode messages = body.putArray("messages");
        for (ChatCompletionRequest.Message m : req.getMessages()) {
            String role = m.getRole() == null ? "user" : m.getRole();
            switch (role) {
                case "system", "developer" -> {
                    if (system.length() > 0) {
                        system.append("\n\n");
                    }
                    system.append(textOf(m.getContent()));
                }
                case "assistant" -> appendMessage(messages, "assistant", assistantBlocks(m));
                case "tool" -> {
                    ObjectNode block = UpstreamHttp.MAPPER.createObjectNode();
                    block.put("type", "tool_result");
                    block.put("tool_use_id", m.getToolCallId());
                    if (m.getContent() instanceof List<?>) {
                        block.set("content", contentBlocks(m.getContent()));
                    } else {
                        block.put("content", textOf(m.getContent()));
                    }
                    ArrayNode blocks = UpstreamHttp.MAPPER.createArrayNode().add(block);
                    appendMessage(messages, "user", blocks);
                }
                default -> appendMessage(messages, "user", contentBlocks(m.getContent()));
            }
        }
        if (system.length() > 0) {
            body.put("system", system.toString());
        }

        if (req.getTemperature() != null) {
            body.put("temperature", req.getTemperature());
        }
        if (req.getTopP() != null) {
            body.put("top_p", req.getTopP());
        }
        if (req.getExtra().get("top_k") instanceof Number k) {
            body.put("top_k", k.intValue());
        }
        if (req.getStop() != null) {
            ArrayNode stops = body.putArray("stop_sequences");
            if (req.getStop() instanceof List<?> l) {
                l.forEach(s -> stops.add(String.valueOf(s)));
            } else {
                stops.add(String.valueOf(req.getStop()));
            }
        }
        if (req.getUser() != null) {
            body.putObject("metadata").put("user_id", req.getUser());
        }

        if (req.getTools() != null && !req.getTools().isEmpty()) {
            ArrayNode tools = body.putArray("tools");
            for (Map<String, Object> tool : req.getTools()) {
                Object fnObj = tool.get("function");
                if (!(fnObj instanceof Map<?, ?> fn)) {
                    continue;
                }
                ObjectNode t = tools.addObject();
                t.put("name", String.valueOf(fn.get("name")));
                if (fn.get("description") != null) {
                    t.put("description", String.valueOf(fn.get("description")));
                }
                Object params = fn.get("parameters");
                if (params != null) {
                    t.set("input_schema", UpstreamHttp.MAPPER.valueToTree(params));
                } else {
                    t.putObject("input_schema").put("type", "object").putObject("properties");
                }
            }
        }

        ObjectNode toolChoice = mapToolChoice(req.getToolChoice());
        if (Boolean.FALSE.equals(req.getParallelToolCalls()) && req.getTools() != null && !req.getTools().isEmpty()) {
            if (toolChoice == null) {
                toolChoice = UpstreamHttp.MAPPER.createObjectNode().put("type", "auto");
            }
            if (!"none".equals(toolChoice.path("type").asText())) {
                toolChoice.put("disable_parallel_tool_use", true);
            }
        }
        if (toolChoice != null) {
            body.set("tool_choice", toolChoice);
        }

        if (stream) {
            body.put("stream", true);
        }
        return body;
    }

    static ObjectNode mapToolChoice(Object toolChoice) {
        if (toolChoice == null) {
            return null;
        }
        ObjectNode tc = UpstreamHttp.MAPPER.createObjectNode();
        if (toolChoice instanceof String s) {
            switch (s) {
                case "auto" -> tc.put("type", "auto");
                case "required", "any" -> tc.put("type", "any");
                case "none" -> tc.put("type", "none");
                default -> {
                    return null;
                }
            }
            return tc;
        }
        if (toolChoice instanceof Map<?, ?> map && map.get("function") instanceof Map<?, ?> fn) {
            tc.put("type", "tool");
            tc.put("name", String.valueOf(fn.get("name")));
            return tc;
        }
        return null;
    }

    private ArrayNode assistantBlocks(ChatCompletionRequest.Message m) {
        ArrayNode blocks = UpstreamHttp.MAPPER.createArrayNode();
        if (m.getContent() instanceof List<?>) {
            contentBlocks(m.getContent()).forEach(blocks::add);
        } else {
            String text = textOf(m.getContent());
            if (!text.isEmpty()) {
                blocks.addObject().put("type", "text").put("text", text);
            }
        }
        if (m.getToolCalls() != null) {
            for (ChatCompletionRequest.ToolCall call : m.getToolCalls()) {
                ObjectNode b = blocks.addObject();
                b.put("type", "tool_use");
                b.put("id", call.getId());
                b.put("name", call.getFunction() == null ? "" : call.getFunction().getName());
                b.set("input", parseArguments(call.getFunction() == null ? null : call.getFunction().getArguments()));
            }
        }
        return blocks;
    }

    private static JsonNode parseArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return UpstreamHttp.MAPPER.createObjectNode();
        }
        try {
            JsonNode n = UpstreamHttp.MAPPER.readTree(arguments);
            return n.isObject() ? n : UpstreamHttp.MAPPER.createObjectNode().set("value", n);
        } catch (JsonProcessingException e) {
            return UpstreamHttp.MAPPER.createObjectNode().put("_raw_arguments", arguments);
        }
    }

    /** Append blocks as a message, merging into the previous message when the role repeats. */
    private static void appendMessage(ArrayNode messages, String role, ArrayNode blocks) {
        if (blocks.isEmpty()) {
            return;
        }
        if (!messages.isEmpty()) {
            ObjectNode last = (ObjectNode) messages.get(messages.size() - 1);
            if (role.equals(last.path("role").asText())) {
                ((ArrayNode) last.get("content")).addAll(blocks);
                return;
            }
        }
        ObjectNode msg = messages.addObject();
        msg.put("role", role);
        msg.set("content", blocks);
    }

    /** OpenAI content (string or parts array) -> Anthropic content blocks. */
    private static ArrayNode contentBlocks(Object content) {
        ArrayNode blocks = UpstreamHttp.MAPPER.createArrayNode();
        if (content == null) {
            return blocks;
        }
        if (content instanceof List<?> parts) {
            for (Object partObj : parts) {
                if (!(partObj instanceof Map<?, ?> part)) {
                    continue;
                }
                String type = String.valueOf(part.get("type"));
                if ("text".equals(type)) {
                    blocks.addObject().put("type", "text").put("text", String.valueOf(part.get("text")));
                } else if ("image_url".equals(type)) {
                    Object iu = part.get("image_url");
                    String url = iu instanceof Map<?, ?> m ? String.valueOf(m.get("url")) : String.valueOf(iu);
                    blocks.add(imageBlock(url));
                }
            }
            return blocks;
        }
        String text = String.valueOf(content);
        if (!text.isEmpty()) {
            blocks.addObject().put("type", "text").put("text", text);
        }
        return blocks;
    }

    private static ObjectNode imageBlock(String url) {
        ObjectNode block = UpstreamHttp.MAPPER.createObjectNode();
        block.put("type", "image");
        ObjectNode source = block.putObject("source");
        if (url.startsWith("data:") && url.contains(";base64,")) {
            int semi = url.indexOf(';');
            source.put("type", "base64");
            source.put("media_type", url.substring(5, semi));
            source.put("data", url.substring(url.indexOf(";base64,") + 8));
        } else {
            source.put("type", "url");
            source.put("url", url);
        }
        return block;
    }

    /** Plain text of an OpenAI content value (string or parts array). */
    static String textOf(Object content) {
        if (content == null) {
            return "";
        }
        if (content instanceof List<?> parts) {
            StringBuilder sb = new StringBuilder();
            for (Object p : parts) {
                if (p instanceof Map<?, ?> m && "text".equals(m.get("type"))) {
                    sb.append(m.get("text"));
                }
            }
            return sb.toString();
        }
        return String.valueOf(content);
    }

    // ------------------------------------------------------------------ response mapping

    static String mapStopReason(String stopReason) {
        if (stopReason == null) {
            return null;
        }
        return switch (stopReason) {
            case "end_turn", "stop_sequence", "pause_turn" -> "stop";
            case "max_tokens" -> "length";
            case "tool_use" -> "tool_calls";
            case "refusal" -> "content_filter";
            default -> "stop";
        };
    }

    public static ChatCompletionResponse toOpenAi(JsonNode msg) {
        StringBuilder text = new StringBuilder();
        StringBuilder thinking = new StringBuilder();
        List<ChatCompletionResponse.ToolCall> toolCalls = new ArrayList<>();
        for (JsonNode block : msg.path("content")) {
            switch (block.path("type").asText()) {
                case "text" -> text.append(block.path("text").asText());
                case "thinking" -> thinking.append(block.path("thinking").asText());
                case "tool_use" -> toolCalls.add(ChatCompletionResponse.ToolCall.builder()
                    .id(block.path("id").asText())
                    .type("function")
                    .function(ChatCompletionResponse.FunctionCall.builder()
                        .name(block.path("name").asText())
                        .arguments(block.path("input").isMissingNode() ? "{}" : block.path("input").toString())
                        .build())
                    .build());
                default -> {
                }
            }
        }
        ChatCompletionResponse.Message message = ChatCompletionResponse.Message.builder()
            .role("assistant")
            .content(text.length() == 0 && !toolCalls.isEmpty() ? null : text.toString())
            .reasoningContent(thinking.length() == 0 ? null : thinking.toString())
            .toolCalls(toolCalls.isEmpty() ? null : toolCalls)
            .build();
        int in = msg.path("usage").path("input_tokens").asInt(0);
        int out = msg.path("usage").path("output_tokens").asInt(0);
        return ChatCompletionResponse.builder()
            .id(msg.path("id").asText("chatcmpl-anthropic"))
            .object("chat.completion")
            .created(System.currentTimeMillis() / 1000)
            .model(msg.path("model").asText())
            .choices(List.of(ChatCompletionResponse.Choice.builder()
                .index(0)
                .message(message)
                .finishReason(mapStopReason(msg.path("stop_reason").asText(null)))
                .build()))
            .usage(ChatCompletionResponse.Usage.builder()
                .promptTokens(in).completionTokens(out).totalTokens(in + out).build())
            .build();
    }

    @Override
    public ChatCompletionResponse complete(UpstreamTarget target, ChatCompletionRequest request) {
        UpstreamProvider p = target.provider();
        HttpResponse<String> resp = http.postJson(BaseUrls.anthropicMessagesUrl(p.baseUrl()),
            headers(target, false), buildBody(request, false).toString(), p.timeoutSeconds());
        UpstreamHttp.ensureOk(resp);
        try {
            return toOpenAi(UpstreamHttp.MAPPER.readTree(resp.body()));
        } catch (JsonProcessingException e) {
            throw new UpstreamException(502, "upstream_invalid_response", "Invalid Anthropic response", true, e);
        }
    }

    // ------------------------------------------------------------------ streaming

    @Override
    public StreamResult stream(UpstreamTarget target, ChatCompletionRequest request, StreamSink sink) {
        UpstreamProvider p = target.provider();
        HttpResponse<Stream<String>> resp = http.postStream(BaseUrls.anthropicMessagesUrl(p.baseUrl()),
            headers(target, true), buildBody(request, true).toString(), p.timeoutSeconds());
        UpstreamHttp.ensureStreamOk(resp);

        boolean includeUsage = request.getStreamOptions() != null
            && Boolean.TRUE.equals(request.getStreamOptions().get("include_usage"));
        StreamTranslator tr = new StreamTranslator(request.getModel(), includeUsage, sink);
        try (Stream<String> lines = resp.body()) {
            Iterator<String> it = lines.iterator();
            while (it.hasNext() && !tr.done) {
                String line = it.next();
                if (line.startsWith("data:")) {
                    String payload = line.substring(5).trim();
                    if (!payload.isEmpty()) {
                        tr.onEvent(payload);
                    }
                }
            }
        } catch (java.io.UncheckedIOException e) {
            throw new UpstreamException(502, "upstream_stream_error", "Upstream stream failed: " + e.getMessage(),
                tr.result.getChunks() == 0, e);
        }
        tr.finish();
        return tr.result;
    }

    /**
     * Translates Anthropic stream events into OpenAI chunks. Package-visible for tests.
     */
    static final class StreamTranslator {
        final StreamResult result = new StreamResult();
        private final boolean includeUsage;
        private final StreamSink sink;
        private final Map<Integer, Integer> blockToToolIndex = new HashMap<>();
        private String id = "chatcmpl-anthropic";
        private String model;
        private final long created = System.currentTimeMillis() / 1000;
        private int nextToolIndex;
        private int inputTokens;
        private int outputTokens;
        private String finishReason;
        private boolean finished;
        boolean done;

        StreamTranslator(String model, boolean includeUsage, StreamSink sink) {
            this.model = model;
            this.includeUsage = includeUsage;
            this.sink = sink;
        }

        void onEvent(String payload) {
            JsonNode ev;
            try {
                ev = UpstreamHttp.MAPPER.readTree(payload);
            } catch (IOException e) {
                log.debug("Skipping unparsable Anthropic event");
                return;
            }
            switch (ev.path("type").asText()) {
                case "message_start" -> {
                    JsonNode msg = ev.path("message");
                    id = msg.path("id").asText(id);
                    model = msg.path("model").asText(model);
                    inputTokens = msg.path("usage").path("input_tokens").asInt(0);
                    ObjectNode delta = UpstreamHttp.MAPPER.createObjectNode();
                    delta.put("role", "assistant");
                    delta.put("content", "");
                    send(OpenAiChunks.chunk(id, model, created, delta, null));
                }
                case "content_block_start" -> {
                    JsonNode block = ev.path("content_block");
                    if ("tool_use".equals(block.path("type").asText())) {
                        int toolIndex = nextToolIndex++;
                        blockToToolIndex.put(ev.path("index").asInt(), toolIndex);
                        ObjectNode delta = UpstreamHttp.MAPPER.createObjectNode();
                        ObjectNode tc = delta.putArray("tool_calls").addObject();
                        tc.put("index", toolIndex);
                        tc.put("id", block.path("id").asText());
                        tc.put("type", "function");
                        tc.putObject("function").put("name", block.path("name").asText()).put("arguments", "");
                        result.getGeneratedText().append(block.path("name").asText());
                        send(OpenAiChunks.chunk(id, model, created, delta, null));
                    } else if ("text".equals(block.path("type").asText()) && !block.path("text").asText("").isEmpty()) {
                        sendContent("content", block.path("text").asText());
                    }
                }
                case "content_block_delta" -> {
                    JsonNode d = ev.path("delta");
                    switch (d.path("type").asText()) {
                        case "text_delta" -> sendContent("content", d.path("text").asText());
                        case "thinking_delta" -> sendContent("reasoning_content", d.path("thinking").asText());
                        case "input_json_delta" -> {
                            Integer toolIndex = blockToToolIndex.get(ev.path("index").asInt());
                            if (toolIndex == null) {
                                return;
                            }
                            String part = d.path("partial_json").asText("");
                            result.getGeneratedText().append(part);
                            ObjectNode delta = UpstreamHttp.MAPPER.createObjectNode();
                            ObjectNode tc = delta.putArray("tool_calls").addObject();
                            tc.put("index", toolIndex);
                            tc.putObject("function").put("arguments", part);
                            send(OpenAiChunks.chunk(id, model, created, delta, null));
                        }
                        default -> {
                        }
                    }
                }
                case "message_delta" -> {
                    String sr = ev.path("delta").path("stop_reason").asText(null);
                    if (sr != null) {
                        finishReason = mapStopReason(sr);
                    }
                    if (ev.path("usage").has("output_tokens")) {
                        outputTokens = ev.path("usage").path("output_tokens").asInt();
                    }
                }
                case "message_stop" -> {
                    finish();
                    done = true;
                }
                case "error" -> {
                    String msg = ev.path("error").path("message").asText("Anthropic stream error");
                    throw new UpstreamException(502, "upstream_stream_error", msg, result.getChunks() == 0);
                }
                default -> {
                    // ping, content_block_stop, signature_delta ...
                }
            }
        }

        private void sendContent(String field, String text) {
            if (text == null || text.isEmpty()) {
                return;
            }
            result.getGeneratedText().append(text);
            ObjectNode delta = UpstreamHttp.MAPPER.createObjectNode();
            delta.put(field, text);
            send(OpenAiChunks.chunk(id, model, created, delta, null));
        }

        void finish() {
            if (finished) {
                return;
            }
            finished = true;
            String fr = finishReason != null ? finishReason : "stop";
            send(OpenAiChunks.chunk(id, model, created, null, fr));
            ChatCompletionResponse.Usage usage = ChatCompletionResponse.Usage.builder()
                .promptTokens(inputTokens).completionTokens(outputTokens)
                .totalTokens(inputTokens + outputTokens).build();
            if (includeUsage) {
                send(OpenAiChunks.usageChunk(id, model, created, usage));
            }
            result.setModel(model);
            result.setFinishReason(fr);
            if (inputTokens > 0 || outputTokens > 0) {
                result.setUsage(usage);
            }
        }

        private void send(ObjectNode chunk) {
            OpenAiCompatibleAdapter.emit(sink, chunk.toString());
            result.setChunks(result.getChunks() + 1);
        }
    }

    @Override
    public List<String> listModels(UpstreamTarget target) {
        UpstreamProvider p = target.provider();
        Map<String, String> h = new LinkedHashMap<>(headers(target, false));
        h.remove("Content-Type");
        HttpResponse<String> resp = http.get(BaseUrls.anthropicModelsUrl(p.baseUrl()), h, 30);
        UpstreamHttp.ensureOk(resp);
        List<String> ids = new ArrayList<>();
        try {
            for (JsonNode m : UpstreamHttp.MAPPER.readTree(resp.body()).path("data")) {
                ids.add(m.path("id").asText());
            }
        } catch (IOException e) {
            throw new UpstreamException(502, "upstream_invalid_response", "Invalid /models response", false, e);
        }
        return ids;
    }
}
