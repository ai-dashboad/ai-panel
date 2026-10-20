package com.aipanel.channel.core.upstream;

/**
 * A resolved upstream provider (from provider_config, or from environment fallback).
 *
 * @param id             provider_config id, or {@code null} for environment-configured fallbacks
 * @param name           display name
 * @param type           provider type (openai, anthropic, vllm, ollama, openai_compatible, ...)
 * @param baseUrl        base URL (may be blank for well-known hosted types)
 * @param timeoutSeconds request timeout
 * @param retryCount     max attempts on this provider (different keys) before failing over
 * @param staticApiKey   API key for environment fallbacks (DB providers use the key pool)
 */
public record UpstreamProvider(Long id, String name, String type, String baseUrl,
                               int timeoutSeconds, int retryCount, String staticApiKey) {
}
