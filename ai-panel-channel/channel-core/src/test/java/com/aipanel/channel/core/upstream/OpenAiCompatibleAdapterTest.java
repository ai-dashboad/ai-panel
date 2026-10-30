package com.aipanel.channel.core.upstream;

import com.aipanel.channel.api.dto.request.ChatCompletionRequest;
import com.aipanel.channel.api.dto.response.ChatCompletionResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleAdapterTest {

    private MockUpstream upstream;
    private final OpenAiCompatibleAdapter adapter = new OpenAiCompatibleAdapter();

    @BeforeEach
    void setUp() throws Exception {
        upstream = new MockUpstream();
    }

    @AfterEach
    void tearDown() {
        upstream.close();
    }

    private UpstreamTarget vllm(String baseUrl) {
        return new UpstreamTarget(new UpstreamProvider(1L, "gpu-1", "vllm", baseUrl, 10, 1, null), null, null);
    }

    static final String TOOL_CALL_RESPONSE = """
        {"id":"chatcmpl-1","object":"chat.completion","created":1700000000,"model":"Qwen/Qwen3-32B",
         "choices":[{"index":0,"message":{"role":"assistant","content":null,"reasoning_content":"need weather",
           "tool_calls":[{"id":"call_9","type":"function","function":{"name":"get_weather","arguments":"{\\"city\\":\\"Paris\\"}"}}]},
           "finish_reason":"tool_calls"}],
         "usage":{"prompt_tokens":50,"completion_tokens":12,"total_tokens":62},
         "kv_transfer_params":null, "prompt_logprobs": null, "service_tier": "default"}
        """;

    @Test
    void forwardsToolConversationVerbatimWithoutForcedDefaults() {
        upstream.json("/v1/chat/completions", 200, TOOL_CALL_RESPONSE);
        ChatCompletionRequest req = TestRequests.parse(TestRequests.TOOL_CONVERSATION);

        // base URL without /v1 -> /v1 is appended
        ChatCompletionResponse resp = adapter.complete(vllm(upstream.baseUrl()), req);

        MockUpstream.Recorded r = upstream.last();
        assertThat(r.path()).isEqualTo("/v1/chat/completions");
        assertThat(r.header("Authorization")).isNull(); // self-hosted, no key
        JsonNode body = r.json();
        assertThat(body.get("model").asText()).isEqualTo("Qwen/Qwen3-32B");
        // no forced sampling defaults
        assertThat(body.has("temperature")).isFalse();
        assertThat(body.has("max_tokens")).isFalse();
        assertThat(body.has("top_p")).isFalse();
        assertThat(body.has("stream")).isFalse();
        // tools passed through untouched (incl. strict)
        assertThat(body.at("/tools/0/function/name").asText()).isEqualTo("get_weather");
        assertThat(body.at("/tools/0/function/strict").asBoolean()).isTrue();
        assertThat(body.at("/tools/0/function/parameters/required/0").asText()).isEqualTo("city");
        assertThat(body.get("tool_choice").asText()).isEqualTo("required");
        assertThat(body.get("parallel_tool_calls").asBoolean()).isFalse();
        // assistant tool_calls and tool results
        assertThat(body.at("/messages/2/tool_calls/1/id").asText()).isEqualTo("call_2");
        assertThat(body.at("/messages/2/tool_calls/1/function/arguments").asText()).isEqualTo("{\"city\":\"Tokyo\"}");
        assertThat(body.at("/messages/3/role").asText()).isEqualTo("tool");
        assertThat(body.at("/messages/3/tool_call_id").asText()).isEqualTo("call_1");
        // content array (image) passed through
        assertThat(body.at("/messages/1/content/1/type").asText()).isEqualTo("image_url");
        assertThat(body.at("/messages/1/content/1/image_url/url").asText()).startsWith("data:image/png;base64,");
        // unknown extra fields passed through
        assertThat(body.get("top_k").asInt()).isEqualTo(20);
        assertThat(body.at("/chat_template_kwargs/enable_thinking").asBoolean()).isFalse();

        // response: tool_calls + real finish_reason
        ChatCompletionResponse.Choice choice = resp.getChoices().get(0);
        assertThat(choice.getFinishReason()).isEqualTo("tool_calls");
        assertThat(choice.getMessage().getToolCalls()).hasSize(1);
        assertThat(choice.getMessage().getToolCalls().get(0).getFunction().getName()).isEqualTo("get_weather");
        assertThat(choice.getMessage().getReasoningContent()).isEqualTo("need weather");
        assertThat(resp.getUsage().getTotalTokens()).isEqualTo(62);
        assertThat(resp.getExtra()).containsEntry("service_tier", "default");
    }

    @Test
    void forwardsClientSamplingParametersWhenSent() {
        upstream.json("/v1/chat/completions", 200, TOOL_CALL_RESPONSE);
        ChatCompletionRequest req = TestRequests.parse("""
            {"model":"glm-4.5","messages":[{"role":"user","content":"hi"}],
             "temperature":0.2,"max_tokens":64,"top_p":0.9,"stop":"END","seed":7}
            """);
        adapter.complete(vllm(upstream.baseUrl() + "/v1/"), req);
        JsonNode body = upstream.last().json();
        assertThat(body.get("temperature").asDouble()).isEqualTo(0.2);
        assertThat(body.get("max_tokens").asInt()).isEqualTo(64);
        assertThat(body.get("top_p").asDouble()).isEqualTo(0.9);
        assertThat(body.get("stop").asText()).isEqualTo("END");
        assertThat(body.get("seed").asInt()).isEqualTo(7);
        assertThat(upstream.last().path()).isEqualTo("/v1/chat/completions");
    }

    @Test
    void streamsToolCallDeltasVerbatim() {
        List<String> events = List.of(
            "data: {\"id\":\"c1\",\"object\":\"chat.completion.chunk\",\"model\":\"deepseek-r1\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"\"},\"finish_reason\":null}]}\n\n",
            "data: {\"id\":\"c1\",\"object\":\"chat.completion.chunk\",\"model\":\"deepseek-r1\",\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call_a\",\"type\":\"function\",\"function\":{\"name\":\"get_weather\",\"arguments\":\"\"}}]},\"finish_reason\":null}]}\n\n",
            "data: {\"id\":\"c1\",\"object\":\"chat.completion.chunk\",\"model\":\"deepseek-r1\",\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"{\\\"city\\\":\"}}]},\"finish_reason\":null}]}\n\n",
            "data: {\"id\":\"c1\",\"object\":\"chat.completion.chunk\",\"model\":\"deepseek-r1\",\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"\\\"Paris\\\"}\"}}]},\"finish_reason\":null}]}\n\n",
            "data: {\"id\":\"c1\",\"object\":\"chat.completion.chunk\",\"model\":\"deepseek-r1\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"tool_calls\"}]}\n\n",
            "data: {\"id\":\"c1\",\"object\":\"chat.completion.chunk\",\"model\":\"deepseek-r1\",\"choices\":[],\"usage\":{\"prompt_tokens\":30,\"completion_tokens\":9,\"total_tokens\":39}}\n\n",
            "data: [DONE]\n\n");
        upstream.sse("/v1/chat/completions", events);
        ChatCompletionRequest req = TestRequests.parse(TestRequests.TOOL_CONVERSATION);
        req.setStream(true);
        req.setStreamOptions(java.util.Map.of("include_usage", true));

        List<String> chunks = new ArrayList<>();
        StreamResult result = adapter.stream(vllm(upstream.baseUrl() + "/v1"), req, chunks::add);

        JsonNode sent = upstream.last().json();
        assertThat(sent.get("stream").asBoolean()).isTrue();
        assertThat(sent.at("/stream_options/include_usage").asBoolean()).isTrue();
        assertThat(sent.has("tools")).isTrue();

        assertThat(chunks).hasSize(6); // [DONE] is not forwarded by the adapter
        assertThat(chunks.get(1)).contains("\"id\":\"call_a\"").contains("\"name\":\"get_weather\"");
        assertThat(chunks.get(2)).contains("{\\\"city\\\":");
        assertThat(result.getFinishReason()).isEqualTo("tool_calls");
        assertThat(result.getUsage().getTotalTokens()).isEqualTo(39);
        assertThat(result.getGeneratedText().toString()).contains("get_weather").contains("Paris");
    }

    @Test
    void convertsPlainJsonWhenUpstreamIgnoresStream() {
        upstream.json("/v1/chat/completions", 200, TOOL_CALL_RESPONSE);
        ChatCompletionRequest req = TestRequests.parse(TestRequests.TOOL_CONVERSATION);
        req.setStream(true);
        List<String> chunks = new ArrayList<>();
        StreamResult result = adapter.stream(vllm(upstream.baseUrl()), req, chunks::add);
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).contains("\"tool_calls\"").contains("\"index\":0").contains("call_9");
        assertThat(chunks.get(1)).contains("\"finish_reason\":\"tool_calls\"");
        assertThat(result.getFinishReason()).isEqualTo("tool_calls");
    }

    @Test
    void mapsUpstreamErrors() {
        upstream.json("/v1/chat/completions", 400, "{\"error\":{\"message\":\"tool_choice requires --enable-auto-tool-choice\"}}");
        ChatCompletionRequest req = TestRequests.parse(TestRequests.TOOL_CONVERSATION);
        assertThatThrownBy(() -> adapter.complete(vllm(upstream.baseUrl()), req))
            .isInstanceOfSatisfying(UpstreamException.class, e -> {
                assertThat(e.getStatus()).isEqualTo(400);
                assertThat(e.isRetryable()).isFalse();
                assertThat(e.getMessage()).contains("enable-auto-tool-choice");
            });

        upstream.json("/v1/chat/completions", 503, "overloaded");
        assertThatThrownBy(() -> adapter.complete(vllm(upstream.baseUrl()), req))
            .isInstanceOfSatisfying(UpstreamException.class, e -> {
                assertThat(e.getStatus()).isEqualTo(502);
                assertThat(e.isRetryable()).isTrue();
            });
    }

    @Test
    void unreachableUpstreamIsRetryable() {
        String deadBase = upstream.baseUrl();
        upstream.close();
        ChatCompletionRequest req = TestRequests.parse("{\"model\":\"m\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}");
        assertThatThrownBy(() -> adapter.complete(vllm(deadBase), req))
            .isInstanceOfSatisfying(UpstreamException.class, e -> assertThat(e.isRetryable()).isTrue());
    }

    @Test
    void sendsBearerKeyWhenConfigured() {
        upstream.json("/v1/chat/completions", 200, TOOL_CALL_RESPONSE);
        UpstreamTarget t = new UpstreamTarget(
            new UpstreamProvider(2L, "compat", "openai_compatible", upstream.baseUrl() + "/v1", 10, 1, null), "sk-test-123", 5L);
        adapter.complete(t, TestRequests.parse("{\"model\":\"m\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}"));
        assertThat(upstream.last().header("Authorization")).isEqualTo("Bearer sk-test-123");
    }

    @Test
    void openAiAdapterRequiresKey() {
        OpenAiAdapter openAi = new OpenAiAdapter();
        UpstreamTarget noKey = new UpstreamTarget(
            new UpstreamProvider(3L, "OpenAI", "openai", upstream.baseUrl(), 10, 1, null), null, null);
        ChatCompletionRequest req = TestRequests.parse("{\"model\":\"gpt-4o\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}");
        assertThatThrownBy(() -> openAi.complete(noKey, req))
            .isInstanceOfSatisfying(UpstreamException.class, e -> assertThat(e.getCode()).isEqualTo("no_api_key"));
        assertThat(upstream.requests).isEmpty();

        upstream.json("/v1/chat/completions", 200, TOOL_CALL_RESPONSE);
        openAi.complete(new UpstreamTarget(noKey.provider(), "sk-openai", 1L), req);
        assertThat(upstream.last().header("Authorization")).isEqualTo("Bearer sk-openai");
        assertThat(openAi.supports("openai")).isTrue();
        assertThat(openAi.supports("vllm")).isFalse();
        assertThat(adapter.supports("vllm")).isTrue();
        assertThat(adapter.supports("ollama")).isTrue();
        assertThat(adapter.supports("openai_compatible")).isTrue();
        assertThat(adapter.supports("anthropic")).isFalse();
    }

    @Test
    void listsModelsFromUpstream() {
        upstream.json("/v1/models", 200, "{\"object\":\"list\",\"data\":[{\"id\":\"Qwen/Qwen3-32B\"},{\"id\":\"deepseek-r1:32b\"}]}");
        assertThat(adapter.listModels(vllm(upstream.baseUrl() + "/v1")))
            .containsExactly("Qwen/Qwen3-32B", "deepseek-r1:32b");
    }
}
