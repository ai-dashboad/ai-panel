package com.aipanel.gateway.core.service;

import com.aipanel.gateway.api.dto.ApiKeyValidationResult;
import reactor.core.publisher.Mono;

/**
 * API Key validation service interface
 */
public interface ApiKeyValidationService {

    /**
     * Validate an API key
     *
     * @param apiKey The API key to validate
     * @return Validation result
     */
    Mono<ApiKeyValidationResult> validate(String apiKey);

    /**
     * Validate API key and check if model is allowed
     *
     * @param apiKey The API key
     * @param model The model to access
     * @return Validation result with model permission check
     */
    Mono<ApiKeyValidationResult> validateWithModel(String apiKey, String model);

    /**
     * Invalidate cached API key info
     *
     * @param keyId Key ID to invalidate
     */
    void invalidateCache(String keyId);
}
