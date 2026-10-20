package com.aipanel.channel.core.upstream;

import lombok.Getter;

/**
 * Error talking to (or caused by) an upstream provider.
 * <p>
 * {@code status} is the HTTP status that should be returned to the client,
 * {@code retryable} tells the router whether another key/provider may be tried.
 */
@Getter
public class UpstreamException extends RuntimeException {

    private final int status;
    private final String code;
    private final boolean retryable;

    public UpstreamException(int status, String code, String message, boolean retryable) {
        super(message);
        this.status = status;
        this.code = code;
        this.retryable = retryable;
    }

    public UpstreamException(int status, String code, String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
        this.retryable = retryable;
    }

    /**
     * Map an upstream HTTP error status to what the client should see.
     * Errors caused by the request itself (400/404/413/422) are passed through and not retried;
     * auth/rate-limit/server errors are retried on other keys/providers and surface as 429/502.
     */
    public static UpstreamException fromUpstreamStatus(int upstreamStatus, String message) {
        String msg = "Upstream returned HTTP " + upstreamStatus + (message == null || message.isBlank() ? "" : ": " + message);
        return switch (upstreamStatus) {
            case 400, 404, 413, 422 -> new UpstreamException(upstreamStatus, "upstream_bad_request", msg, false);
            case 429 -> new UpstreamException(429, "upstream_rate_limited", msg, true);
            default -> new UpstreamException(502, "upstream_error", msg, true);
        };
    }
}
