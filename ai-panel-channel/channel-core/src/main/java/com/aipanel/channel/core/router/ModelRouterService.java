package com.aipanel.channel.core.router;

import com.aipanel.channel.api.dto.request.ChatCompletionRequest;
import com.aipanel.channel.api.dto.response.ChatCompletionResponse;
import com.aipanel.channel.api.dto.response.ModelListResponse;
import com.aipanel.channel.core.upstream.StreamResult;
import com.aipanel.channel.core.upstream.StreamSink;
import com.aipanel.channel.core.upstream.UpstreamProvider;

import java.util.List;

/**
 * Routes chat completion requests to the configured upstream providers.
 */
public interface ModelRouterService {

    /**
     * Non-streaming chat completion.
     */
    ChatCompletionResponse chatCompletion(ChatCompletionRequest request);

    /**
     * Streaming chat completion. OpenAI-format chunks are written to {@code sink}
     * (the caller writes the final {@code [DONE]}).
     */
    StreamResult streamChatCompletion(ChatCompletionRequest request, StreamSink sink);

    /**
     * Models available through enabled providers.
     */
    ModelListResponse listModels();

    /**
     * Enabled model ids of all enabled providers of a type.
     */
    List<String> getProviderModels(String providerType);

    /**
     * Whether at least one usable provider can serve this model.
     */
    boolean isModelSupported(String model);

    /**
     * Candidate providers for a model, in the order they will be tried.
     */
    List<UpstreamProvider> resolveProviders(String model);
}
