package com.aipanel.channel.core.upstream;

import java.net.URI;

/**
 * Base URL normalisation for upstream providers.
 * <ul>
 *   <li>{@code http://gpu-host:8000} and {@code http://gpu-host:8000/v1} both resolve to
 *       {@code http://gpu-host:8000/v1/chat/completions}</li>
 *   <li>a base URL that already has a path (e.g. {@code /api/paas/v4}) is used as-is</li>
 *   <li>Anthropic: {@code https://api.anthropic.com} or {@code .../v1} resolve to {@code .../v1/messages}</li>
 * </ul>
 */
public final class BaseUrls {

    private BaseUrls() {
    }

    /**
     * Normalised OpenAI-style API root, e.g. {@code http://host:8000/v1}.
     */
    public static String openAiRoot(String baseUrl, String type) {
        String b = baseUrl == null || baseUrl.isBlank() ? ProviderTypes.defaultBaseUrl(type) : baseUrl.trim();
        if (b == null || b.isBlank()) {
            throw new UpstreamException(400, "invalid_provider_config",
                "Provider of type '" + type + "' has no base URL configured", false);
        }
        b = stripTrailingSlash(b);
        if (b.endsWith("/chat/completions")) {
            b = b.substring(0, b.length() - "/chat/completions".length());
        }
        String path = parse(b).getPath();
        if (path == null || path.isEmpty() || "/".equals(path)) {
            b = b + "/v1";
        }
        return b;
    }

    public static String chatCompletionsUrl(String baseUrl, String type) {
        return openAiRoot(baseUrl, type) + "/chat/completions";
    }

    public static String modelsUrl(String baseUrl, String type) {
        return openAiRoot(baseUrl, type) + "/models";
    }

    public static String anthropicMessagesUrl(String baseUrl) {
        String b = baseUrl == null || baseUrl.isBlank()
            ? ProviderTypes.defaultBaseUrl(ProviderTypes.ANTHROPIC) : baseUrl.trim();
        b = stripTrailingSlash(b);
        parse(b);
        if (b.endsWith("/messages")) {
            return b;
        }
        if (b.endsWith("/v1")) {
            return b + "/messages";
        }
        return b + "/v1/messages";
    }

    public static String anthropicModelsUrl(String baseUrl) {
        String messages = anthropicMessagesUrl(baseUrl);
        return messages.substring(0, messages.length() - "/messages".length()) + "/models";
    }

    private static String stripTrailingSlash(String s) {
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static URI parse(String s) {
        try {
            URI uri = URI.create(s);
            if (uri.getScheme() == null || uri.getHost() == null
                    || !(uri.getScheme().equals("http") || uri.getScheme().equals("https"))) {
                throw new IllegalArgumentException("not an http(s) URL");
            }
            return uri;
        } catch (IllegalArgumentException e) {
            throw new UpstreamException(400, "invalid_provider_config", "Invalid provider base URL: " + s, false);
        }
    }
}
