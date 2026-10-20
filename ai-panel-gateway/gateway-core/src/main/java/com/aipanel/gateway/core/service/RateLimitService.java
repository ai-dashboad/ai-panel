package com.aipanel.gateway.core.service;

import com.aipanel.gateway.api.dto.RateLimitInfo;
import reactor.core.publisher.Mono;

/**
 * Rate limiting service interface
 */
public interface RateLimitService {

    /**
     * Check and consume rate limit for global requests
     *
     * @return Rate limit info
     */
    Mono<RateLimitInfo> checkGlobalRateLimit();

    /**
     * Check and consume rate limit for a user
     *
     * @param userId User ID
     * @param planType User's plan type
     * @return Rate limit info
     */
    Mono<RateLimitInfo> checkUserRateLimit(Long userId, String planType);

    /**
     * Check and consume rate limit for an API key
     *
     * @param keyId API Key ID
     * @param customLimit Custom rate limit (null to use default)
     * @return Rate limit info
     */
    Mono<RateLimitInfo> checkApiKeyRateLimit(String keyId, Integer customLimit);

    /**
     * Check and consume rate limit for a specific model
     *
     * @param model Model name
     * @return Rate limit info
     */
    Mono<RateLimitInfo> checkModelRateLimit(String model);

    /**
     * Get current rate limit info without consuming
     *
     * @param key Rate limit key
     * @return Rate limit info
     */
    Mono<RateLimitInfo> getRateLimitInfo(String key);
}
