package com.aipanel.key.core.service;

import com.aipanel.common.domain.PageResult;
import com.aipanel.key.api.dto.request.CreateApiKeyRequest;
import com.aipanel.key.api.dto.request.UpdateApiKeyRequest;
import com.aipanel.key.api.dto.response.ApiKeyResponse;
import com.aipanel.key.api.dto.response.ApiKeyValidationResponse;
import com.aipanel.key.api.dto.response.QuotaUsageResponse;

/**
 * API Key service interface
 */
public interface ApiKeyService {

    /**
     * Create a new API Key
     *
     * @param userId User ID
     * @param request Create request
     * @return Created API Key response (includes full key only on creation)
     */
    ApiKeyResponse createApiKey(Long userId, CreateApiKeyRequest request);

    /**
     * List API Keys for a user
     *
     * @param userId User ID
     * @param pageNum Page number
     * @param pageSize Page size
     * @return Paginated API Keys
     */
    PageResult<ApiKeyResponse> listApiKeys(Long userId, int pageNum, int pageSize);

    /**
     * Get API Key by ID
     *
     * @param userId User ID
     * @param keyId Key ID
     * @return API Key response
     */
    ApiKeyResponse getApiKey(Long userId, String keyId);

    /**
     * Update API Key
     *
     * @param userId User ID
     * @param keyId Key ID
     * @param request Update request
     * @return Updated API Key response
     */
    ApiKeyResponse updateApiKey(Long userId, String keyId, UpdateApiKeyRequest request);

    /**
     * Delete API Key
     *
     * @param userId User ID
     * @param keyId Key ID
     */
    void deleteApiKey(Long userId, String keyId);

    /**
     * Regenerate API Key secret
     *
     * @param userId User ID
     * @param keyId Key ID
     * @return API Key response with new full key
     */
    ApiKeyResponse regenerateApiKey(Long userId, String keyId);

    /**
     * Get usage statistics for an API Key
     *
     * @param userId User ID
     * @param keyId Key ID
     * @return Usage statistics
     */
    QuotaUsageResponse getKeyUsage(Long userId, String keyId);

    /**
     * Validate API Key (internal use)
     *
     * @param apiKey Full API Key
     * @param model Optional model to check permission
     * @return Validation result
     */
    ApiKeyValidationResponse validateApiKey(String apiKey, String model);

    /**
     * Update last used time
     *
     * @param keyId Key ID
     */
    void updateLastUsed(String keyId);

    /**
     * Ensure user has a default API Key.
     * If user has no keys, creates one automatically.
     * Returns existing key (without full key) or newly created key (with full key).
     *
     * @param userId User ID
     * @return API Key response (fullKey only included if newly created)
     */
    ApiKeyResponse ensureDefaultApiKey(Long userId);
}
