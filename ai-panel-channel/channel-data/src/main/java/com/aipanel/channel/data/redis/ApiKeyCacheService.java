package com.aipanel.channel.data.redis;

import com.aipanel.channel.data.mysql.po.ChannelApiKeyPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * API key cache service using Redis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyCacheService {

    private static final String KEY_PREFIX = "channel:key:";
    private static final String HASH_PREFIX = "channel:hash:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Cache API key data
     */
    public void cacheKey(ChannelApiKeyPO key) {
        try {
            String json = objectMapper.writeValueAsString(key);
            redisTemplate.opsForValue().set(KEY_PREFIX + key.getKeyId(), json, CACHE_TTL);
            redisTemplate.opsForValue().set(HASH_PREFIX + key.getKeyHash(), key.getKeyId(), CACHE_TTL);
        } catch (Exception e) {
            log.error("Failed to cache API key: {}", key.getKeyId(), e);
        }
    }

    /**
     * Get cached key by key ID
     */
    public Optional<ChannelApiKeyPO> getByKeyId(String keyId) {
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + keyId);
            if (json != null) {
                return Optional.of(objectMapper.readValue(json, ChannelApiKeyPO.class));
            }
        } catch (Exception e) {
            log.error("Failed to get cached API key: {}", keyId, e);
        }
        return Optional.empty();
    }

    /**
     * Get key ID by hash
     */
    public Optional<String> getKeyIdByHash(String keyHash) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(HASH_PREFIX + keyHash));
    }

    /**
     * Invalidate key cache
     */
    public void invalidate(String keyId, String keyHash) {
        redisTemplate.delete(KEY_PREFIX + keyId);
        redisTemplate.delete(HASH_PREFIX + keyHash);
    }

    /**
     * Check rate limit
     * @return true if request is allowed, false if rate limited
     */
    public boolean checkRateLimit(String keyId, int limit) {
        String rateLimitKey = "channel:ratelimit:" + keyId;
        Long current = redisTemplate.opsForValue().increment(rateLimitKey);

        if (current == 1) {
            redisTemplate.expire(rateLimitKey, Duration.ofMinutes(1));
        }

        return current <= limit;
    }

    /**
     * Update used quota in cache
     */
    public void updateUsedQuota(String keyId, long usedQuota) {
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + keyId);
            if (json != null) {
                ChannelApiKeyPO key = objectMapper.readValue(json, ChannelApiKeyPO.class);
                key.setUsedQuota(usedQuota);
                redisTemplate.opsForValue().set(
                    KEY_PREFIX + keyId,
                    objectMapper.writeValueAsString(key),
                    CACHE_TTL
                );
            }
        } catch (Exception e) {
            log.error("Failed to update quota cache: {}", keyId, e);
        }
    }
}
