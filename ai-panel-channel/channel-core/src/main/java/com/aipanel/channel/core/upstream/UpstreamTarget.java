package com.aipanel.channel.core.upstream;

/**
 * A provider plus the concrete API key chosen for one attempt.
 *
 * @param provider the provider
 * @param apiKey   API key, may be {@code null} for key-optional (self-hosted) providers
 * @param keyId    provider_api_key id for usage/error accounting, may be {@code null}
 */
public record UpstreamTarget(UpstreamProvider provider, String apiKey, Long keyId) {

    public static UpstreamTarget of(String type, String baseUrl, String apiKey) {
        return new UpstreamTarget(new UpstreamProvider(null, type, type, baseUrl, 60, 1, apiKey), apiKey, null);
    }
}
