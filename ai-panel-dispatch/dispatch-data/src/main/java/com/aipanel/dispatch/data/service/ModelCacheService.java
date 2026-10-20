package com.aipanel.dispatch.data.service;

import com.aipanel.dispatch.data.po.ModelInfoPO;
import com.aipanel.dispatch.data.po.ModelProviderPO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Model Cache Service
 * Caches model and provider information in Redis
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ModelCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String MODEL_KEY_PREFIX = "dispatch:model:";
    private static final String PROVIDER_KEY_PREFIX = "dispatch:provider:";
    private static final String MODEL_LIST_KEY = "dispatch:models:all";
    private static final String PROVIDER_LIST_KEY = "dispatch:providers:all";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    /**
     * Cache model info
     */
    public void cacheModel(ModelInfoPO model) {
        try {
            String key = MODEL_KEY_PREFIX + model.getModelId();
            String value = objectMapper.writeValueAsString(model);
            redisTemplate.opsForValue().set(key, value, DEFAULT_TTL);
        } catch (Exception e) {
            log.error("Failed to cache model {}: {}", model.getModelId(), e.getMessage());
        }
    }

    /**
     * Get cached model info
     */
    public ModelInfoPO getCachedModel(String modelId) {
        try {
            String key = MODEL_KEY_PREFIX + modelId;
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                return objectMapper.readValue(value, ModelInfoPO.class);
            }
        } catch (Exception e) {
            log.error("Failed to get cached model {}: {}", modelId, e.getMessage());
        }
        return null;
    }

    /**
     * Cache all enabled models
     */
    public void cacheAllModels(List<ModelInfoPO> models) {
        try {
            String value = objectMapper.writeValueAsString(models);
            redisTemplate.opsForValue().set(MODEL_LIST_KEY, value, DEFAULT_TTL);

            // Also cache individually
            for (ModelInfoPO model : models) {
                cacheModel(model);
            }
        } catch (Exception e) {
            log.error("Failed to cache model list: {}", e.getMessage());
        }
    }

    /**
     * Get all cached models
     */
    public List<ModelInfoPO> getCachedAllModels() {
        try {
            String value = redisTemplate.opsForValue().get(MODEL_LIST_KEY);
            if (value != null) {
                return objectMapper.readValue(value, new TypeReference<List<ModelInfoPO>>() {});
            }
        } catch (Exception e) {
            log.error("Failed to get cached models: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Cache provider info
     */
    public void cacheProvider(ModelProviderPO provider) {
        try {
            String key = PROVIDER_KEY_PREFIX + provider.getProviderCode();
            String value = objectMapper.writeValueAsString(provider);
            redisTemplate.opsForValue().set(key, value, DEFAULT_TTL);
        } catch (Exception e) {
            log.error("Failed to cache provider {}: {}", provider.getProviderCode(), e.getMessage());
        }
    }

    /**
     * Get cached provider info
     */
    public ModelProviderPO getCachedProvider(String providerCode) {
        try {
            String key = PROVIDER_KEY_PREFIX + providerCode;
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                return objectMapper.readValue(value, ModelProviderPO.class);
            }
        } catch (Exception e) {
            log.error("Failed to get cached provider {}: {}", providerCode, e.getMessage());
        }
        return null;
    }

    /**
     * Cache all enabled providers
     */
    public void cacheAllProviders(List<ModelProviderPO> providers) {
        try {
            String value = objectMapper.writeValueAsString(providers);
            redisTemplate.opsForValue().set(PROVIDER_LIST_KEY, value, DEFAULT_TTL);

            for (ModelProviderPO provider : providers) {
                cacheProvider(provider);
            }
        } catch (Exception e) {
            log.error("Failed to cache provider list: {}", e.getMessage());
        }
    }

    /**
     * Invalidate model cache
     */
    public void invalidateModel(String modelId) {
        redisTemplate.delete(MODEL_KEY_PREFIX + modelId);
        redisTemplate.delete(MODEL_LIST_KEY);
    }

    /**
     * Invalidate provider cache
     */
    public void invalidateProvider(String providerCode) {
        redisTemplate.delete(PROVIDER_KEY_PREFIX + providerCode);
        redisTemplate.delete(PROVIDER_LIST_KEY);
    }

    /**
     * Invalidate all cache
     */
    public void invalidateAll() {
        redisTemplate.delete(redisTemplate.keys("dispatch:*"));
    }
}
