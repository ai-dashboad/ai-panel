package com.aipanel.common.constant;

/**
 * Common constants used across the platform
 */
public final class CommonConstants {

    private CommonConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    // API Key prefixes
    public static final String API_KEY_PREFIX = "sk-";
    public static final int API_KEY_LENGTH = 32;
    public static final int API_KEY_DISPLAY_PREFIX_LENGTH = 7;
    public static final int API_KEY_DISPLAY_SUFFIX_LENGTH = 4;

    // HTTP Headers
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_API_KEY = "X-API-Key";
    public static final String HEADER_REQUEST_ID = "X-Request-ID";
    public static final String HEADER_USER_ID = "X-User-ID";
    public static final String HEADER_RATE_LIMIT = "X-RateLimit-Limit";
    public static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    public static final String HEADER_RATE_LIMIT_RESET = "X-RateLimit-Reset";
    public static final String HEADER_RETRY_AFTER = "Retry-After";

    // Bearer token prefix
    public static final String BEARER_PREFIX = "Bearer ";

    // Cache key prefixes
    public static final String CACHE_PREFIX = "ai-panel:";
    public static final String CACHE_API_KEY = CACHE_PREFIX + "api-key:";
    public static final String CACHE_USER = CACHE_PREFIX + "user:";
    public static final String CACHE_MODEL = CACHE_PREFIX + "model:";
    public static final String CACHE_RATE_LIMIT = CACHE_PREFIX + "rate-limit:";
    public static final String CACHE_QUOTA = CACHE_PREFIX + "quota:";
    public static final String CACHE_CIRCUIT_BREAKER = CACHE_PREFIX + "circuit-breaker:";

    // Cache TTL (in seconds)
    public static final long CACHE_TTL_API_KEY = 300;       // 5 minutes
    public static final long CACHE_TTL_USER = 600;          // 10 minutes
    public static final long CACHE_TTL_MODEL = 3600;        // 1 hour
    public static final long CACHE_TTL_RATE_LIMIT = 60;     // 1 minute

    // Pagination defaults
    public static final int DEFAULT_PAGE_NUM = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // Request limits
    public static final int MAX_MESSAGES = 100;
    public static final int MAX_MESSAGE_LENGTH = 100000;
    public static final int MAX_STOP_SEQUENCES = 4;
    public static final int DEFAULT_MAX_TOKENS = 2048;
    public static final double DEFAULT_TEMPERATURE = 0.7;

    // Time constants (in milliseconds)
    public static final long ONE_SECOND = 1000L;
    public static final long ONE_MINUTE = 60 * ONE_SECOND;
    public static final long ONE_HOUR = 60 * ONE_MINUTE;
    public static final long ONE_DAY = 24 * ONE_HOUR;

    // Default timeouts (in milliseconds)
    public static final int DEFAULT_CONNECTION_TIMEOUT = 5000;
    public static final int DEFAULT_READ_TIMEOUT = 60000;
    public static final int DEFAULT_WRITE_TIMEOUT = 60000;

    // Status codes
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_INACTIVE = 0;
    public static final int STATUS_DELETED = -1;

    // Role names
    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_FUNCTION = "function";
    public static final String ROLE_TOOL = "tool";
}
