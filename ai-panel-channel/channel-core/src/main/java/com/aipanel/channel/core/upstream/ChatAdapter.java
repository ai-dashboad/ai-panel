package com.aipanel.channel.core.upstream;

import com.aipanel.channel.api.dto.request.ChatCompletionRequest;
import com.aipanel.channel.api.dto.response.ChatCompletionResponse;

import java.util.List;

/**
 * Talks to one upstream wire protocol and always speaks OpenAI chat completions to the caller.
 */
public interface ChatAdapter {

    /** Whether this adapter handles the given provider type. */
    boolean supports(String providerType);

    ChatCompletionResponse complete(UpstreamTarget target, ChatCompletionRequest request);

    /**
     * Stream a completion. Each OpenAI-format chunk is passed to {@code sink}; the caller writes
     * the terminating {@code [DONE]}.
     */
    StreamResult stream(UpstreamTarget target, ChatCompletionRequest request, StreamSink sink);

    /** List model ids served by the upstream (GET /models). */
    List<String> listModels(UpstreamTarget target);
}
