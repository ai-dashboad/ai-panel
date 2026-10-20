package com.aipanel.key.data.redis;

import com.aipanel.common.constant.CommonConstants;
import com.aipanel.common.util.JsonUtils;
import com.aipanel.key.api.dto.response.ApiKeyValidationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * API Key cache service using Redis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyCacheService {

    private final StringRedisTemplate redisTemplate;

    private static final String VALIDATION_PREFIX = CommonConstants.CACHE_API_KEY + "validation:";

    /**
     * Get cached validation result
     *
     * @param keyHash API Key hash
     * @return Validation result or null if not cached
     */
    public ApiKeyValidationResponse getValidation(String keyHash) {
        String key = VALIDATION_PREFIX + keyHash;
        String json = redisTemplate.opsForValue().get(key);
        if (json != null) {
            log.debug("API Key validation cache hit: hash={}", keyHash.substring(0, 8));
            return JsonUtils.fromJson(json, ApiKeyValidationResponse.class);
        }
        return null;
    }

    /**
     * Cache validation result
     *
     * @param keyHash API Key hash
     * @param response Validation result
     */
    public void cacheValidation(String keyHash, ApiKeyValidationResponse response) {
        String key = VALIDATION_PREFIX + keyHash;
        String json = JsonUtils.toJson(response);
        if (json != null) {
            redisTemplate.opsForValue().set(key, json,
                CommonConstants.CACHE_TTL_API_KEY, TimeUnit.SECONDS);
            log.debug("API Key validation cached: hash={}", keyHash.substring(0, 8));
        }
    }

    /**
     * Invalidate cached validation
     *
     * @param keyId API Key ID
     */
    public void invalidate(String keyId) {
        // Note: We need to invalidate by keyId, but cache is by hash
        // In production, you'd also maintain a keyId -> hash mapping
        // For now, we'll just log the invalidation
        log.info("API Key cache invalidation requested: keyId={}", keyId);

        // Pattern-based invalidation (if needed)
        // This is a simplified approach; production would use more efficient methods
    }

    /**
     * Invalidate by key hash
     *
     * @param keyHash API Key hash
     */
    public void invalidateByHash(String keyHash) {
        String key = VALIDATION_PREFIX + keyHash;
        redisTemplate.delete(key);
        log.debug("API Key validation cache invalidated: hash={}", keyHash.substring(0, 8));
    }
}
