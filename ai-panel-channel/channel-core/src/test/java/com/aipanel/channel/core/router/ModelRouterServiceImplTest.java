package com.aipanel.channel.core.router;

import com.aipanel.channel.api.dto.request.ChatCompletionRequest;
import com.aipanel.channel.api.dto.response.ChatCompletionResponse;
import com.aipanel.channel.core.pool.ApiKeyPool;
import com.aipanel.channel.core.router.impl.ModelRouterServiceImpl;
import com.aipanel.channel.core.upstream.AnthropicAdapter;
import com.aipanel.channel.core.upstream.MockUpstream;
import com.aipanel.channel.core.upstream.OpenAiAdapter;
import com.aipanel.channel.core.upstream.OpenAiCompatibleAdapter;
import com.aipanel.channel.core.upstream.StreamResult;
import com.aipanel.channel.core.upstream.UpstreamException;
import com.aipanel.channel.core.upstream.UpstreamHttp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModelRouterServiceImplTest {

    private static final String OK = """
        {"id":"r1","object":"chat.completion","created":1,"model":"%s",
         "choices":[{"index":0,"message":{"role":"assistant","content":null,
           "tool_calls":[{"id":"call_1","type":"function","function":{"name":"get_weather","arguments":"{}"}}]},
           "finish_reason":"tool_calls"}],
         "usage":{"prompt_tokens":3,"completion_tokens":4,"total_tokens":7}}
        """;

    private MockUpstream gpu1;
    private MockUpstream gpu2;
    private ApiKeyPool pool;
    private ModelRouterServiceImpl router;

    @BeforeEach
    void setUp() throws Exception {
        gpu1 = new MockUpstream();
        gpu2 = new MockUpstream();
        pool = mock(ApiKeyPool.class);
        router = new ModelRouterServiceImpl(pool,
            List.of(new OpenAiAdapter(), new AnthropicAdapter(), new OpenAiCompatibleAdapter()));
        when(pool.getProvidersForModel(anyString())).thenReturn(List.of());
        when(pool.getEnabledProvidersByType(anyString())).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        gpu1.close();
        gpu2.close();
    }

    private static ApiKeyPool.ProviderConfig provider(long id, String type, String baseUrl, int priority) {
        ApiKeyPool.ProviderConfig c = new ApiKeyPool.ProviderConfig();
        c.setId(id);
        c.setName(type + "-" + id);
        c.setType(type);
        c.setBaseUrl(baseUrl);
        c.setTimeoutSeconds(10);
        c.setRetryCount(3);
        c.setPriority(priority);
        return c;
    }

    private static ApiKeyPool.KeyEntry key(long id, String value) {
        ApiKeyPool.KeyEntry k = new ApiKeyPool.KeyEntry();
        k.setId(id);
        k.setApiKey(value);
        return k;
    }

    private static ChatCompletionRequest request(String model) throws Exception {
        return UpstreamHttp.MAPPER.readValue("""
            {"model":"%s","messages":[{"role":"user","content":"weather?"}],
             "tools":[{"type":"function","function":{"name":"get_weather","parameters":{"type":"object","properties":{}}}}]}
            """.formatted(model), ChatCompletionRequest.class);
    }

    @Test
    void routesArbitraryModelNameToConfiguredSelfHostedProvider() throws Exception {
        ApiKeyPool.ProviderConfig vllm = provider(10, "vllm", gpu1.baseUrl() + "/v1", 100);
        when(pool.getProvidersForModel("Qwen/Qwen3-32B")).thenReturn(List.of(vllm));
        gpu1.json("/v1/chat/completions", 200, OK.formatted("Qwen/Qwen3-32B"));

        assertThat(router.isModelSupported("Qwen/Qwen3-32B")).isTrue();
        ChatCompletionResponse resp = router.chatCompletion(request("Qwen/Qwen3-32B"));

        assertThat(resp.getChoices().get(0).getFinishReason()).isEqualTo("tool_calls");
        assertThat(gpu1.last().json().get("model").asText()).isEqualTo("Qwen/Qwen3-32B");
        assertThat(gpu1.last().json().has("tools")).isTrue();
        assertThat(gpu1.last().header("Authorization")).isNull();
    }

    @Test
    void unknownModelIsNotSupported() throws Exception {
        assertThat(router.isModelSupported("my-private-finetune")).isFalse();
        assertThatThrownBy(() -> router.chatCompletion(request("my-private-finetune")))
            .isInstanceOfSatisfying(UpstreamException.class, e -> {
                assertThat(e.getStatus()).isEqualTo(404);
                assertThat(e.getCode()).isEqualTo("model_not_found");
            });
    }

    @Test
    void providerThatNeedsAKeyButHasNoneIsSkipped() {
        ApiKeyPool.ProviderConfig deepseek = provider(11, "deepseek", gpu1.baseUrl(), 100);
        when(pool.getProvidersForModel("deepseek-chat")).thenReturn(List.of(deepseek));
        when(pool.hasAvailableKeys(11L)).thenReturn(false);
        assertThat(router.isModelSupported("deepseek-chat")).isFalse();
    }

    @Test
    void prefixFallbackStillWorksForUnlistedModels() throws Exception {
        ApiKeyPool.ProviderConfig openai = provider(1, "openai", gpu1.baseUrl(), 100);
        when(pool.getEnabledProvidersByType("openai")).thenReturn(List.of(openai));
        when(pool.hasAvailableKeys(1L)).thenReturn(true);
        when(pool.getKeyCount(1L)).thenReturn(1);
        when(pool.getNextKey(1L)).thenReturn(key(100, "sk-openai-test"));
        gpu1.json("/v1/chat/completions", 200, OK.formatted("gpt-4o-mini"));

        router.chatCompletion(request("gpt-4o-mini"));

        assertThat(gpu1.last().header("Authorization")).isEqualTo("Bearer sk-openai-test");
        verify(pool).recordSuccess(100L, 7L);
    }

    @Test
    void failsOverToNextProviderOnRetryableError() throws Exception {
        ApiKeyPool.ProviderConfig a = provider(20, "openai_compatible", gpu1.baseUrl() + "/v1", 10);
        ApiKeyPool.ProviderConfig b = provider(21, "vllm", gpu2.baseUrl() + "/v1", 20);
        when(pool.getProvidersForModel("glm-4.5")).thenReturn(List.of(a, b));
        when(pool.hasAvailableKeys(20L)).thenReturn(true);
        when(pool.getKeyCount(20L)).thenReturn(1);
        when(pool.getNextKey(20L)).thenReturn(key(200, "sk-a"));
        gpu1.json("/v1/chat/completions", 500, "{\"error\":{\"message\":\"CUDA out of memory\"}}");
        gpu2.json("/v1/chat/completions", 200, OK.formatted("glm-4.5"));

        ChatCompletionResponse resp = router.chatCompletion(request("glm-4.5"));

        assertThat(resp.getModel()).isEqualTo("glm-4.5");
        assertThat(gpu1.requests).hasSize(1);
        assertThat(gpu2.requests).hasSize(1);
        verify(pool).recordError(eq(200L), anyString());
    }

    @Test
    void doesNotFailOverOnClientErrors() throws Exception {
        ApiKeyPool.ProviderConfig a = provider(30, "vllm", gpu1.baseUrl(), 10);
        ApiKeyPool.ProviderConfig b = provider(31, "vllm", gpu2.baseUrl(), 20);
        when(pool.getProvidersForModel("deepseek-r1")).thenReturn(List.of(a, b));
        gpu1.json("/v1/chat/completions", 400, "{\"error\":{\"message\":\"bad tool schema\"}}");

        assertThatThrownBy(() -> router.chatCompletion(request("deepseek-r1")))
            .isInstanceOfSatisfying(UpstreamException.class, e -> assertThat(e.getStatus()).isEqualTo(400));
        assertThat(gpu2.requests).isEmpty();
        verify(pool, never()).recordError(anyLong(), anyString());
    }

    @Test
    void streamsThroughConfiguredProvider() throws Exception {
        ApiKeyPool.ProviderConfig ollama = provider(40, "ollama", gpu1.baseUrl(), 100);
        when(pool.getProvidersForModel("deepseek-r1:32b")).thenReturn(List.of(ollama));
        gpu1.sse("/v1/chat/completions", List.of(
            "data: {\"id\":\"s\",\"object\":\"chat.completion.chunk\",\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call_x\",\"type\":\"function\",\"function\":{\"name\":\"get_weather\",\"arguments\":\"{}\"}}]},\"finish_reason\":null}]}\n\n",
            "data: {\"id\":\"s\",\"object\":\"chat.completion.chunk\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"tool_calls\"}]}\n\n",
            "data: [DONE]\n\n"));
        ChatCompletionRequest req = request("deepseek-r1:32b");
        req.setStream(true);
        List<String> chunks = new ArrayList<>();

        StreamResult result = router.streamChatCompletion(req, chunks::add);

        assertThat(chunks).hasSize(2);
        assertThat(result.getFinishReason()).isEqualTo("tool_calls");
        assertThat(gpu1.last().json().get("stream").asBoolean()).isTrue();
    }

    @Test
    void environmentFallbackForOpenAiAndAnthropic() throws Exception {
        router.setEnvFallbacks(true, "sk-env-openai", gpu1.baseUrl(), true, "sk-placeholder", gpu2.baseUrl());
        gpu1.json("/v1/chat/completions", 200, OK.formatted("gpt-4.1"));

        router.chatCompletion(request("gpt-4.1"));
        assertThat(gpu1.last().header("Authorization")).isEqualTo("Bearer sk-env-openai");
        // placeholder key is ignored
        assertThat(router.isModelSupported("claude-sonnet-4-5")).isFalse();
    }

    @Test
    void listsModelsOfUsableProviders() {
        ApiKeyPool.ProviderConfig vllm = provider(50, "vllm", gpu1.baseUrl(), 100);
        ApiKeyPool.ProviderConfig openaiNoKey = provider(51, "openai", null, 100);
        when(pool.getEnabledProviders()).thenReturn(List.of(vllm, openaiNoKey));
        when(pool.getModelsForProvider(50L)).thenReturn(List.of("Qwen/Qwen3-32B", "glm-4.5"));
        when(pool.getModelsForProvider(51L)).thenReturn(List.of("gpt-4o"));

        assertThat(router.listModels().getData()).extracting(m -> m.getId())
            .containsExactly("Qwen/Qwen3-32B", "glm-4.5");
    }
}
