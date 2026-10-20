package com.aipanel.key.core.service.impl;

import com.aipanel.common.domain.PageResult;
import com.aipanel.common.enums.ErrorCode;
import com.aipanel.common.enums.PlanType;
import com.aipanel.common.exception.ApiKeyException;
import com.aipanel.common.exception.BusinessException;
import com.aipanel.common.util.ApiKeyGenerator;
import com.aipanel.common.util.JsonUtils;
import com.aipanel.key.api.dto.request.CreateApiKeyRequest;
import com.aipanel.key.api.dto.request.UpdateApiKeyRequest;
import com.aipanel.key.api.dto.response.ApiKeyResponse;
import com.aipanel.key.api.dto.response.ApiKeyValidationResponse;
import com.aipanel.key.api.dto.response.QuotaUsageResponse;
import com.aipanel.key.core.service.ApiKeyService;
import com.aipanel.key.data.mysql.po.ApiKeyPO;
import com.aipanel.key.data.mysql.service.ApiKeyDataService;
import com.aipanel.key.data.redis.ApiKeyCacheService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API Key service implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyDataService apiKeyDataService;
    private final ApiKeyCacheService apiKeyCacheService;

    private static final int MAX_KEYS_PER_USER = 10;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiKeyResponse createApiKey(Long userId, CreateApiKeyRequest request) {
        // Check key limit
        long keyCount = apiKeyDataService.countByUserId(userId);
        if (keyCount >= MAX_KEYS_PER_USER) {
            throw BusinessException.of(ErrorCode.API_KEY_LIMIT_REACHED,
                "Maximum " + MAX_KEYS_PER_USER + " API Keys allowed per user");
        }

        // Generate new API Key
        String fullKey = ApiKeyGenerator.generate();
        String keyHash = ApiKeyGenerator.hash(fullKey);
        String keyId = ApiKeyGenerator.extractKeyId(fullKey);
        String keyPrefix = ApiKeyGenerator.getDisplayPrefix(fullKey);

        // Create record
        ApiKeyPO apiKey = new ApiKeyPO();
        apiKey.setUserId(userId);
        apiKey.setKeyId(keyId);
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setName(request.getName());
        apiKey.setDescription(request.getDescription());
        apiKey.setStatus(1);
        apiKey.setAllowedModels(request.getAllowedModels() != null ?
            JsonUtils.toJson(request.getAllowedModels()) : null);
        apiKey.setRateLimitOverride(request.getRateLimitPerMinute());
        apiKey.setExpiresAt(request.getExpiresAt());
        apiKey.setCreatedAt(LocalDateTime.now());
        apiKey.setUpdatedAt(LocalDateTime.now());

        apiKeyDataService.save(apiKey);
        log.info("API Key created: userId={}, keyId={}", userId, keyId);

        // Return response with full key (only time it's returned)
        return convertToResponse(apiKey, fullKey);
    }

    @Override
    public PageResult<ApiKeyResponse> listApiKeys(Long userId, int pageNum, int pageSize) {
        Page<ApiKeyPO> page = new Page<>(pageNum, pageSize);
        IPage<ApiKeyPO> result = apiKeyDataService.findPageByUserId(page, userId);

        List<ApiKeyResponse> list = result.getRecords().stream()
            .map(po -> convertToResponse(po, null))
            .collect(Collectors.toList());

        return PageResult.of(list, pageNum, pageSize, result.getTotal());
    }

    @Override
    public ApiKeyResponse getApiKey(Long userId, String keyId) {
        ApiKeyPO apiKey = apiKeyDataService.findByKeyIdAndUserId(keyId, userId);
        if (apiKey == null) {
            throw ApiKeyException.notFound(keyId);
        }
        return convertToResponse(apiKey, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiKeyResponse updateApiKey(Long userId, String keyId, UpdateApiKeyRequest request) {
        ApiKeyPO apiKey = apiKeyDataService.findByKeyIdAndUserId(keyId, userId);
        if (apiKey == null) {
            throw ApiKeyException.notFound(keyId);
        }

        // Update fields
        if (request.getName() != null) {
            apiKey.setName(request.getName());
        }
        if (request.getDescription() != null) {
            apiKey.setDescription(request.getDescription());
        }
        if (request.getAllowedModels() != null) {
            apiKey.setAllowedModels(JsonUtils.toJson(request.getAllowedModels()));
        }
        if (request.getRateLimitPerMinute() != null) {
            apiKey.setRateLimitOverride(request.getRateLimitPerMinute());
        }
        if (request.getExpiresAt() != null) {
            apiKey.setExpiresAt(request.getExpiresAt());
        }
        if (request.getStatus() != null) {
            apiKey.setStatus(request.getStatus());
        }
        apiKey.setUpdatedAt(LocalDateTime.now());

        apiKeyDataService.updateById(apiKey);

        // Invalidate cache
        apiKeyCacheService.invalidate(keyId);

        log.info("API Key updated: keyId={}", keyId);
        return convertToResponse(apiKey, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteApiKey(Long userId, String keyId) {
        ApiKeyPO apiKey = apiKeyDataService.findByKeyIdAndUserId(keyId, userId);
        if (apiKey == null) {
            throw ApiKeyException.notFound(keyId);
        }

        apiKeyDataService.removeById(apiKey.getId());

        // Invalidate cache
        apiKeyCacheService.invalidate(keyId);

        log.info("API Key deleted: keyId={}", keyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiKeyResponse regenerateApiKey(Long userId, String keyId) {
        ApiKeyPO apiKey = apiKeyDataService.findByKeyIdAndUserId(keyId, userId);
        if (apiKey == null) {
            throw ApiKeyException.notFound(keyId);
        }

        // Generate new key
        String fullKey = ApiKeyGenerator.generate();
        String keyHash = ApiKeyGenerator.hash(fullKey);
        String newKeyId = ApiKeyGenerator.extractKeyId(fullKey);
        String keyPrefix = ApiKeyGenerator.getDisplayPrefix(fullKey);

        apiKey.setKeyId(newKeyId);
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setUpdatedAt(LocalDateTime.now());

        apiKeyDataService.updateById(apiKey);

        // Invalidate old cache
        apiKeyCacheService.invalidate(keyId);

        log.info("API Key regenerated: oldKeyId={}, newKeyId={}", keyId, newKeyId);
        return convertToResponse(apiKey, fullKey);
    }

    @Override
    public QuotaUsageResponse getKeyUsage(Long userId, String keyId) {
        ApiKeyPO apiKey = apiKeyDataService.findByKeyIdAndUserId(keyId, userId);
        if (apiKey == null) {
            throw ApiKeyException.notFound(keyId);
        }

        // TODO: Implement actual usage statistics from billing service
        return QuotaUsageResponse.builder()
            .keyId(keyId)
            .requestsLimit(10000L)
            .requestsUsed(0L)
            .requestsRemaining(10000L)
            .tokensLimit(1000000L)
            .tokensUsed(0L)
            .tokensRemaining(1000000L)
            .build();
    }

    @Override
    public ApiKeyValidationResponse validateApiKey(String apiKey, String model) {
        // Validate format
        if (!ApiKeyGenerator.isValidFormat(apiKey)) {
            return ApiKeyValidationResponse.invalid(
                ErrorCode.API_KEY_INVALID.getCode(), "Invalid API Key format");
        }

        // Try to get from cache first
        String keyHash = ApiKeyGenerator.hash(apiKey);
        ApiKeyValidationResponse cached = apiKeyCacheService.getValidation(keyHash);
        if (cached != null) {
            return checkModelPermission(cached, model);
        }

        // Query database
        ApiKeyPO keyPO = apiKeyDataService.findByKeyHash(keyHash);
        if (keyPO == null) {
            return ApiKeyValidationResponse.invalid(
                ErrorCode.API_KEY_INVALID.getCode(), "API Key not found");
        }

        // Check status
        if (keyPO.getStatus() != 1) {
            return ApiKeyValidationResponse.invalid(
                ErrorCode.API_KEY_DISABLED.getCode(), "API Key is disabled");
        }

        // Check expiration
        if (keyPO.getExpiresAt() != null && keyPO.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ApiKeyValidationResponse.invalid(
                ErrorCode.API_KEY_EXPIRED.getCode(), "API Key has expired");
        }

        // Build validation response
        List<String> allowedModels = keyPO.getAllowedModels() != null ?
            JsonUtils.fromJsonList(keyPO.getAllowedModels(), String.class) : null;

        ApiKeyValidationResponse response = ApiKeyValidationResponse.valid(
            keyPO.getUserId(),
            keyPO.getKeyId(),
            PlanType.FREE.getCode(), // TODO: Get actual plan from user service
            allowedModels,
            keyPO.getRateLimitOverride()
        );

        // Cache the result
        apiKeyCacheService.cacheValidation(keyHash, response);

        return checkModelPermission(response, model);
    }

    @Override
    public void updateLastUsed(String keyId) {
        apiKeyDataService.updateLastUsed(keyId, LocalDateTime.now());
    }

    private ApiKeyValidationResponse checkModelPermission(ApiKeyValidationResponse response, String model) {
        if (model == null || !response.isValid()) {
            return response;
        }

        List<String> allowedModels = response.getAllowedModels();
        boolean modelAllowed = allowedModels == null || allowedModels.isEmpty() ||
                              allowedModels.contains(model);

        return response.withModelCheck(modelAllowed);
    }

    private ApiKeyResponse convertToResponse(ApiKeyPO po, String fullKey) {
        List<String> allowedModels = po.getAllowedModels() != null ?
            JsonUtils.fromJsonList(po.getAllowedModels(), String.class) : null;

        return ApiKeyResponse.builder()
            .id(po.getKeyId())
            .keyPrefix(po.getKeyPrefix())
            .fullKey(fullKey)
            .name(po.getName())
            .description(po.getDescription())
            .status(po.getStatus())
            .allowedModels(allowedModels)
            .rateLimitPerMinute(po.getRateLimitOverride())
            .expiresAt(po.getExpiresAt())
            .lastUsedAt(po.getLastUsedAt())
            .createdAt(po.getCreatedAt())
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiKeyResponse ensureDefaultApiKey(Long userId) {
        // Check if user already has any API key
        long keyCount = apiKeyDataService.countByUserId(userId);
        if (keyCount > 0) {
            // Return existing first key (without full key)
            Page<ApiKeyPO> page = new Page<>(1, 1);
            IPage<ApiKeyPO> result = apiKeyDataService.findPageByUserId(page, userId);
            if (!result.getRecords().isEmpty()) {
                return convertToResponse(result.getRecords().get(0), null);
            }
        }

        // Create new default API key
        log.info("Creating default API Key for user: {}", userId);

        String fullKey = ApiKeyGenerator.generate();
        String keyHash = ApiKeyGenerator.hash(fullKey);
        String keyId = ApiKeyGenerator.extractKeyId(fullKey);
        String keyPrefix = ApiKeyGenerator.getDisplayPrefix(fullKey);

        ApiKeyPO apiKey = new ApiKeyPO();
        apiKey.setUserId(userId);
        apiKey.setKeyId(keyId);
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setName("Default API Key");
        apiKey.setDescription("Auto-generated API key for quick start");
        apiKey.setStatus(1);
        apiKey.setCreatedAt(LocalDateTime.now());
        apiKey.setUpdatedAt(LocalDateTime.now());

        apiKeyDataService.save(apiKey);
        log.info("Default API Key created: userId={}, keyId={}", userId, keyId);

        // Return with full key (only time it's revealed)
        return convertToResponse(apiKey, fullKey);
    }
}
