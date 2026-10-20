package com.aipanel.channel.core.service.impl;

import com.aipanel.channel.api.dto.request.GenerateApiKeysRequest;
import com.aipanel.channel.api.dto.response.GeneratedKeyResponse;
import com.aipanel.channel.core.service.ApiKeyGeneratorService;
import com.aipanel.channel.data.mysql.po.ChannelApiKeyPO;
import com.aipanel.channel.data.mysql.po.ChannelPO;
import com.aipanel.channel.data.mysql.service.ChannelApiKeyDataService;
import com.aipanel.channel.data.mysql.service.ChannelDataService;
import com.aipanel.channel.data.redis.ApiKeyCacheService;
import com.aipanel.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * API key generator service implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyGeneratorServiceImpl implements ApiKeyGeneratorService {

    private static final String KEY_PREFIX = "sk-";
    private static final int KEY_LENGTH = 48;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ChannelDataService channelDataService;
    private final ChannelApiKeyDataService keyDataService;
    private final ApiKeyCacheService cacheService;

    @Override
    @Transactional
    public List<GeneratedKeyResponse> generateKeys(GenerateApiKeysRequest request) {
        // Validate channel exists and is enabled
        ChannelPO channel = channelDataService.findById(request.getChannelId())
            .orElseThrow(() -> new BusinessException("Channel not found"));

        if (channel.getStatus() != 1) {
            throw new BusinessException("Channel is disabled");
        }

        String prefix = channel.getKeyPrefix() != null
            ? channel.getKeyPrefix()
            : KEY_PREFIX;

        List<ChannelApiKeyPO> keys = new ArrayList<>();
        List<GeneratedKeyResponse> responses = new ArrayList<>();

        for (int i = 0; i < request.getQuantity(); i++) {
            // Generate unique key
            String rawKey = generateRawKey();
            String fullKey = prefix + rawKey;
            String keyHash = hashKey(fullKey);
            String keyId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

            // Create key entity
            ChannelApiKeyPO key = new ChannelApiKeyPO();
            key.setChannelId(request.getChannelId());
            key.setKeyId(keyId);
            key.setKeyHash(keyHash);
            key.setKeyPrefix(fullKey.substring(0, Math.min(8, fullKey.length())) + "...");
            key.setName(request.getNamePrefix() != null
                ? request.getNamePrefix() + "-" + (i + 1)
                : "Key-" + keyId.substring(0, 8));
            key.setPlanType(request.getPlanType() != null ? request.getPlanType() : "basic");
            key.setMonthlyQuota(request.getMonthlyQuota() != null ? request.getMonthlyQuota() : 1000000L);
            key.setUsedQuota(0L);
            key.setRateLimit(request.getRateLimit() != null ? request.getRateLimit() : 60);
            key.setAllowedModels(request.getAllowedModels() != null
                ? String.join(",", request.getAllowedModels())
                : null);
            key.setStatus(1); // active
            key.setPrice(request.getPrice());
            key.setExpiresAt(request.getExpiresAt());

            keys.add(key);

            // Create response with full key (only shown once)
            GeneratedKeyResponse response = new GeneratedKeyResponse();
            response.setKeyId(keyId);
            response.setApiKey(fullKey);
            response.setKeyPrefix(key.getKeyPrefix());
            response.setName(key.getName());
            response.setPlanType(key.getPlanType());
            response.setMonthlyQuota(key.getMonthlyQuota());
            response.setRateLimit(key.getRateLimit());
            response.setExpiresAt(key.getExpiresAt());
            response.setCreatedAt(LocalDateTime.now());

            responses.add(response);
        }

        // Batch insert
        keyDataService.createBatch(keys);

        // Update channel stats
        long totalKeys = keyDataService.countByChannel(request.getChannelId());
        long activeKeys = keyDataService.countActiveByChannel(request.getChannelId());
        channelDataService.updateKeyStats(request.getChannelId(), totalKeys, activeKeys);

        log.info("Generated {} API keys for channel {}", request.getQuantity(), request.getChannelId());
        return responses;
    }

    @Override
    public ChannelApiKeyPO validateKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new BusinessException("API key is required");
        }

        // Remove "Bearer " prefix if present
        if (apiKey.startsWith("Bearer ")) {
            apiKey = apiKey.substring(7);
        }

        String keyHash = hashKey(apiKey);

        // Try cache first
        String cachedKeyId = cacheService.getKeyIdByHash(keyHash).orElse(null);
        if (cachedKeyId != null) {
            ChannelApiKeyPO cached = cacheService.getByKeyId(cachedKeyId).orElse(null);
            if (cached != null) {
                return validateKeyData(cached);
            }
        }

        // Lookup from database
        ChannelApiKeyPO key = keyDataService.findByKeyHash(keyHash)
            .orElseThrow(() -> new BusinessException("Invalid API key"));

        // Cache the key
        cacheService.cacheKey(key);

        return validateKeyData(key);
    }

    private ChannelApiKeyPO validateKeyData(ChannelApiKeyPO key) {
        // Check status
        if (key.getStatus() != 1) {
            throw new BusinessException("API key is disabled");
        }

        // Check expiration
        if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("API key has expired");
        }

        // Check quota
        if (key.getUsedQuota() >= key.getMonthlyQuota()) {
            throw new BusinessException("API key quota exceeded");
        }

        return key;
    }

    @Override
    public boolean hasQuota(String keyId, int tokensNeeded) {
        ChannelApiKeyPO key = keyDataService.findByKeyId(keyId)
            .orElseThrow(() -> new BusinessException("Key not found"));
        return (key.getUsedQuota() + tokensNeeded) <= key.getMonthlyQuota();
    }

    @Override
    @Transactional
    public void consumeQuota(String keyId, int tokens) {
        keyDataService.incrementUsedQuota(keyId, tokens);
        // Update cache
        keyDataService.findByKeyId(keyId).ifPresent(key -> {
            cacheService.updateUsedQuota(keyId, key.getUsedQuota());
        });
    }

    private String generateRawKey() {
        byte[] bytes = new byte[KEY_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
            .substring(0, KEY_LENGTH);
    }

    private String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash API key", e);
        }
    }
}
