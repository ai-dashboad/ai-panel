package com.aipanel.channel.core.router.impl;

import com.aipanel.channel.api.dto.request.ChatCompletionRequest;
import com.aipanel.channel.api.dto.response.ChatCompletionResponse;
import com.aipanel.channel.api.dto.response.ModelListResponse;
import com.aipanel.channel.core.pool.ApiKeyPool;
import com.aipanel.channel.core.router.ModelRouterService;
import com.aipanel.channel.core.upstream.ChatAdapter;
import com.aipanel.channel.core.upstream.ProviderTypes;
import com.aipanel.channel.core.upstream.StreamResult;
import com.aipanel.channel.core.upstream.StreamSink;
import com.aipanel.channel.core.upstream.UpstreamException;
import com.aipanel.channel.core.upstream.UpstreamProvider;
import com.aipanel.channel.core.upstream.UpstreamTarget;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Model router.
 * <p>
 * Resolution order for a model name:
 * <ol>
 *   <li>enabled providers that explicitly list the model (provider_model), by provider priority;
 *       any model name works, e.g. {@code Qwen/Qwen3-32B}, {@code glm-4.5}, {@code deepseek-r1}</li>
 *   <li>prefix fallback: enabled providers whose type is inferred from the model name
 *       ({@code gpt-*} -> openai, {@code claude-*} -> anthropic, ...)</li>
 *   <li>environment fallback: {@code OPENAI_API_KEY} / {@code ANTHROPIC_API_KEY} when
 *       {@code OPENAI_ENABLED} / {@code ANTHROPIC_ENABLED} are true</li>
 * </ol>
 * A provider is usable if it has an available API key, or if its type does not need one
 * (vllm, ollama, openai_compatible, ...). Retryable failures (connection errors, 429, 5xx,
 * upstream auth errors) fail over to the next key / provider. Streams only fail over before
 * the first chunk was sent.
 */
@Slf4j
@Service
public class ModelRouterServiceImpl implements ModelRouterService {

    private final ApiKeyPool apiKeyPool;
    private final List<ChatAdapter> adapters;

    @Value("${spring.ai.openai.enabled:false}")
    private boolean envOpenAiEnabled;
    @Value("${spring.ai.openai.api-key:}")
    private String envOpenAiKey;
    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String envOpenAiBaseUrl;
    @Value("${spring.ai.anthropic.enabled:false}")
    private boolean envAnthropicEnabled;
    @Value("${spring.ai.anthropic.api-key:}")
    private String envAnthropicKey;
    @Value("${spring.ai.anthropic.base-url:https://api.anthropic.com}")
    private String envAnthropicBaseUrl;

    public ModelRouterServiceImpl(ApiKeyPool apiKeyPool, List<ChatAdapter> adapters) {
        this.apiKeyPool = apiKeyPool;
        this.adapters = adapters;
    }

    /** Configure the environment fallbacks (used by tests; Spring injects the @Value fields). */
    public void setEnvFallbacks(boolean openAiEnabled, String openAiKey, String openAiBaseUrl,
                                boolean anthropicEnabled, String anthropicKey, String anthropicBaseUrl) {
        this.envOpenAiEnabled = openAiEnabled;
        this.envOpenAiKey = openAiKey;
        this.envOpenAiBaseUrl = openAiBaseUrl;
        this.envAnthropicEnabled = anthropicEnabled;
        this.envAnthropicKey = anthropicKey;
        this.envAnthropicBaseUrl = anthropicBaseUrl;
    }

    // ------------------------------------------------------------------ resolution

    @Override
    public List<UpstreamProvider> resolveProviders(String model) {
        Map<String, UpstreamProvider> out = new LinkedHashMap<>();

        for (ApiKeyPool.ProviderConfig c : apiKeyPool.getProvidersForModel(model)) {
            if (isUsable(c)) {
                out.putIfAbsent("db:" + c.getId(), toUpstream(c));
            }
        }

        String inferred = ProviderTypes.inferFromModel(model);
        if (inferred != null) {
            for (ApiKeyPool.ProviderConfig c : apiKeyPool.getEnabledProvidersByType(inferred)) {
                if (isUsable(c)) {
                    out.putIfAbsent("db:" + c.getId(), toUpstream(c));
                }
            }
            if (ProviderTypes.OPENAI.equals(inferred) && envOpenAiEnabled && isRealKey(envOpenAiKey)) {
                out.putIfAbsent("env:openai", new UpstreamProvider(null, "OpenAI (env)", ProviderTypes.OPENAI,
                    envOpenAiBaseUrl, 120, 1, envOpenAiKey));
            }
            if (ProviderTypes.ANTHROPIC.equals(inferred) && envAnthropicEnabled && isRealKey(envAnthropicKey)) {
                out.putIfAbsent("env:anthropic", new UpstreamProvider(null, "Anthropic (env)", ProviderTypes.ANTHROPIC,
                    envAnthropicBaseUrl, 120, 1, envAnthropicKey));
            }
        }
        return new ArrayList<>(out.values());
    }

    private boolean isUsable(ApiKeyPool.ProviderConfig c) {
        if (!ProviderTypes.isSupported(c.getType())) {
            return false;
        }
        return ProviderTypes.isKeyOptional(c.getType()) || apiKeyPool.hasAvailableKeys(c.getId());
    }

    private static boolean isRealKey(String key) {
        return key != null && !key.isBlank() && !key.contains("placeholder") && !key.contains("YOUR");
    }

    private static UpstreamProvider toUpstream(ApiKeyPool.ProviderConfig c) {
        return new UpstreamProvider(c.getId(), c.getName(), c.getType(), c.getBaseUrl(),
            c.getTimeoutSeconds(), Math.max(1, c.getRetryCount()), null);
    }

    private ChatAdapter adapterFor(String type) {
        for (ChatAdapter a : adapters) {
            if (a.supports(type)) {
                return a;
            }
        }
        throw new UpstreamException(400, "unsupported_provider_type", "Unsupported provider type: " + type, false);
    }

    /** Number of attempts on one provider: one per available key, bounded by retryCount. */
    private int attemptsFor(UpstreamProvider p) {
        if (p.id() == null) {
            return 1;
        }
        int keys = apiKeyPool.getKeyCount(p.id());
        return Math.max(1, Math.min(p.retryCount(), keys));
    }

    /** Pick the key for one attempt; returns null if a required key is missing. */
    private UpstreamTarget acquire(UpstreamProvider p) {
        if (p.id() == null) {
            return new UpstreamTarget(p, p.staticApiKey(), null);
        }
        ApiKeyPool.KeyEntry key = apiKeyPool.getNextKey(p.id());
        if (key != null) {
            return new UpstreamTarget(p, key.getApiKey(), key.getId());
        }
        if (ProviderTypes.isKeyOptional(p.type())) {
            return new UpstreamTarget(p, null, null);
        }
        return null;
    }

    private void onError(UpstreamTarget t, UpstreamException e) {
        log.warn("Upstream call failed: provider={} type={} status={} retryable={} message={}",
            t.provider().name(), t.provider().type(), e.getStatus(), e.isRetryable(), e.getMessage());
        if (t.keyId() != null && e.isRetryable()) {
            apiKeyPool.recordError(t.keyId(), e.getMessage());
        }
    }

    private List<UpstreamProvider> requireProviders(String model) {
        List<UpstreamProvider> providers = resolveProviders(model);
        if (providers.isEmpty()) {
            throw new UpstreamException(404, "model_not_found",
                "The model '" + model + "' is not configured on any enabled provider", false);
        }
        return providers;
    }

    // ------------------------------------------------------------------ execution

    @Override
    public ChatCompletionResponse chatCompletion(ChatCompletionRequest request) {
        UpstreamException last = null;
        for (UpstreamProvider p : requireProviders(request.getModel())) {
            ChatAdapter adapter = adapterFor(p.type());
            for (int attempt = 0; attempt < attemptsFor(p); attempt++) {
                UpstreamTarget target = acquire(p);
                if (target == null) {
                    break;
                }
                try {
                    ChatCompletionResponse response = adapter.complete(target, request);
                    if (target.keyId() != null) {
                        long tokens = response.getUsage() != null && response.getUsage().getTotalTokens() != null
                            ? response.getUsage().getTotalTokens() : 0;
                        apiKeyPool.recordSuccess(target.keyId(), tokens);
                    }
                    return response;
                } catch (UpstreamException e) {
                    onError(target, e);
                    if (!e.isRetryable()) {
                        throw e;
                    }
                    last = e;
                }
            }
        }
        throw last != null ? last
            : new UpstreamException(503, "no_available_provider",
                "No provider with an available API key for model '" + request.getModel() + "'", false);
    }

    @Override
    public StreamResult streamChatCompletion(ChatCompletionRequest request, StreamSink sink) {
        AtomicBoolean started = new AtomicBoolean(false);
        StreamSink tracking = chunk -> {
            started.set(true);
            sink.send(chunk);
        };
        UpstreamException last = null;
        for (UpstreamProvider p : requireProviders(request.getModel())) {
            ChatAdapter adapter = adapterFor(p.type());
            for (int attempt = 0; attempt < attemptsFor(p); attempt++) {
                UpstreamTarget target = acquire(p);
                if (target == null) {
                    break;
                }
                try {
                    StreamResult result = adapter.stream(target, request, tracking);
                    if (target.keyId() != null) {
                        long tokens = result.getUsage() != null && result.getUsage().getTotalTokens() != null
                            ? result.getUsage().getTotalTokens() : 0;
                        apiKeyPool.recordSuccess(target.keyId(), tokens);
                    }
                    return result;
                } catch (UpstreamException e) {
                    onError(target, e);
                    if (!e.isRetryable() || started.get()) {
                        throw e;
                    }
                    last = e;
                }
            }
        }
        throw last != null ? last
            : new UpstreamException(503, "no_available_provider",
                "No provider with an available API key for model '" + request.getModel() + "'", false);
    }

    // ------------------------------------------------------------------ models

    @Override
    public ModelListResponse listModels() {
        long created = Instant.now().getEpochSecond();
        Map<String, ModelListResponse.ModelData> models = new LinkedHashMap<>();
        for (ApiKeyPool.ProviderConfig c : apiKeyPool.getEnabledProviders()) {
            if (!isUsable(c)) {
                continue;
            }
            for (String id : apiKeyPool.getModelsForProvider(c.getId())) {
                models.putIfAbsent(id, ModelListResponse.ModelData.builder()
                    .id(id).object("model").created(created).ownedBy(c.getType()).build());
            }
        }
        return ModelListResponse.builder().object("list").data(new ArrayList<>(models.values())).build();
    }

    @Override
    public List<String> getProviderModels(String providerType) {
        List<String> out = new ArrayList<>();
        for (ApiKeyPool.ProviderConfig c : apiKeyPool.getEnabledProvidersByType(providerType)) {
            for (String id : apiKeyPool.getModelsForProvider(c.getId())) {
                if (!out.contains(id)) {
                    out.add(id);
                }
            }
        }
        return out;
    }

    @Override
    public boolean isModelSupported(String model) {
        return !resolveProviders(model).isEmpty();
    }
}
