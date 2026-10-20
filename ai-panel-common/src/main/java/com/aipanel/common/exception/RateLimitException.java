package com.aipanel.common.exception;

import com.aipanel.common.enums.ErrorCode;
import lombok.Getter;

/**
 * Exception for rate limiting scenarios
 */
@Getter
public class RateLimitException extends BusinessException {

    private static final long serialVersionUID = 1L;

    private final long retryAfterMs;
    private final String limitType;
    private final long limit;
    private final long remaining;

    public RateLimitException(ErrorCode errorCode, long retryAfterMs) {
        super(errorCode);
        this.retryAfterMs = retryAfterMs;
        this.limitType = null;
        this.limit = 0;
        this.remaining = 0;
    }

    public RateLimitException(ErrorCode errorCode, String message, long retryAfterMs) {
        super(errorCode, message);
        this.retryAfterMs = retryAfterMs;
        this.limitType = null;
        this.limit = 0;
        this.remaining = 0;
    }

    public RateLimitException(ErrorCode errorCode, String limitType, long limit, long remaining, long retryAfterMs) {
        super(errorCode, String.format("Rate limit exceeded for %s: %d/%d, retry after %d ms",
            limitType, limit - remaining, limit, retryAfterMs));
        this.retryAfterMs = retryAfterMs;
        this.limitType = limitType;
        this.limit = limit;
        this.remaining = remaining;
    }

    /**
     * Global rate limit exceeded
     */
    public static RateLimitException global(long retryAfterMs) {
        return new RateLimitException(ErrorCode.GLOBAL_RATE_LIMIT, retryAfterMs);
    }

    /**
     * User rate limit exceeded
     */
    public static RateLimitException user(long limit, long remaining, long retryAfterMs) {
        return new RateLimitException(ErrorCode.USER_RATE_LIMIT, "user", limit, remaining, retryAfterMs);
    }

    /**
     * API Key rate limit exceeded
     */
    public static RateLimitException apiKey(String keyId, long limit, long remaining, long retryAfterMs) {
        return new RateLimitException(ErrorCode.RATE_LIMIT_EXCEEDED,
            "api_key:" + keyId, limit, remaining, retryAfterMs);
    }

    /**
     * Model rate limit exceeded
     */
    public static RateLimitException model(String model, long limit, long remaining, long retryAfterMs) {
        return new RateLimitException(ErrorCode.MODEL_RATE_LIMIT,
            "model:" + model, limit, remaining, retryAfterMs);
    }

    /**
     * Get retry-after header value in seconds
     */
    public long getRetryAfterSeconds() {
        return (retryAfterMs + 999) / 1000;
    }
}
