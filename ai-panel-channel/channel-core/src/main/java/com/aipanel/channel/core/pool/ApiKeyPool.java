package com.aipanel.channel.core.pool;

import com.aipanel.channel.data.mysql.mapper.ProviderApiKeyMapper;
import com.aipanel.channel.data.mysql.mapper.ProviderConfigMapper;
import com.aipanel.channel.data.mysql.po.ProviderApiKeyPO;
import com.aipanel.channel.data.mysql.po.ProviderConfigPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * API Key Pool - manages multiple API keys for each provider
 * Supports weighted round-robin load balancing and automatic failover
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyPool {

    private final ProviderConfigMapper providerConfigMapper;
    private final ProviderApiKeyMapper providerApiKeyMapper;

    /**
     * Cache: providerType -> list of active keys
     */
    private final Map<String, List<KeyEntry>> keyPool = new ConcurrentHashMap<>();

    /**
     * Round-robin counter for each provider
     */
    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();

    /**
     * Provider configuration cache
     */
    private final Map<String, ProviderConfig> providerConfigs = new ConcurrentHashMap<>();

    @Data
    public static class KeyEntry {
        private Long id;
        private String apiKey;
        private String keyName;
        private int weight;
        private int effectiveWeight;
        private int currentWeight;
        private Integer rateLimitPerMinute;
        private Long dailyQuota;
        private Long usedQuotaToday;
        private LocalDateTime lastUsedAt;
        private int consecutiveErrors;
        private String status;
    }

    @Data
    public static class ProviderConfig {
        private Long id;
        private String name;
        private String type;
        private String baseUrl;
        private int timeoutSeconds;
        private int retryCount;
        private int priority;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing API Key Pool...");
        reload();
    }

    /**
     * Reload all provider configurations and keys from database
     */
    @Scheduled(fixedRate = 60000) // Reload every minute
    public void reload() {
        try {
            // Load provider configurations
            List<ProviderConfigPO> providers = providerConfigMapper.selectList(
                new LambdaQueryWrapper<ProviderConfigPO>()
                    .eq(ProviderConfigPO::getEnabled, true)
            );

            Map<String, ProviderConfig> newConfigs = new HashMap<>();
            for (ProviderConfigPO po : providers) {
                ProviderConfig config = new ProviderConfig();
                config.setId(po.getId());
                config.setName(po.getName());
                config.setType(po.getType());
                config.setBaseUrl(po.getBaseUrl());
                config.setTimeoutSeconds(po.getTimeoutSeconds() != null ? po.getTimeoutSeconds() : 60);
                config.setRetryCount(po.getRetryCount() != null ? po.getRetryCount() : 3);
                config.setPriority(po.getPriority() != null ? po.getPriority() : 100);
                newConfigs.put(po.getType(), config);
            }
            providerConfigs.clear();
            providerConfigs.putAll(newConfigs);

            // Load API keys for each provider
            Map<String, List<KeyEntry>> newPool = new HashMap<>();
            for (ProviderConfigPO provider : providers) {
                List<ProviderApiKeyPO> keys = providerApiKeyMapper.selectList(
                    new LambdaQueryWrapper<ProviderApiKeyPO>()
                        .eq(ProviderApiKeyPO::getProviderId, provider.getId())
                        .eq(ProviderApiKeyPO::getEnabled, true)
                        .ne(ProviderApiKeyPO::getStatus, "disabled")
                        .and(wrapper -> wrapper
                            .isNull(ProviderApiKeyPO::getExpiresAt)
                            .or()
                            .gt(ProviderApiKeyPO::getExpiresAt, LocalDateTime.now())
                        )
                );

                List<KeyEntry> entries = keys.stream()
                    .map(this::toKeyEntry)
                    .collect(Collectors.toList());

                if (!entries.isEmpty()) {
                    newPool.put(provider.getType(), entries);
                    roundRobinCounters.putIfAbsent(provider.getType(), new AtomicInteger(0));
                    log.info("Loaded {} keys for provider: {}", entries.size(), provider.getType());
                }
            }

            keyPool.clear();
            keyPool.putAll(newPool);

            log.info("API Key Pool reloaded: {} providers with keys", keyPool.size());

        } catch (Exception e) {
            log.error("Failed to reload API Key Pool", e);
        }
    }

    private KeyEntry toKeyEntry(ProviderApiKeyPO po) {
        KeyEntry entry = new KeyEntry();
        entry.setId(po.getId());
        entry.setApiKey(po.getApiKey());
        entry.setKeyName(po.getKeyName());
        entry.setWeight(po.getWeight() != null ? po.getWeight() : 100);
        entry.setEffectiveWeight(entry.getWeight());
        entry.setCurrentWeight(0);
        entry.setRateLimitPerMinute(po.getRateLimitPerMinute());
        entry.setDailyQuota(po.getDailyQuota());
        entry.setUsedQuotaToday(po.getUsedQuotaToday() != null ? po.getUsedQuotaToday() : 0L);
        entry.setLastUsedAt(po.getLastUsedAt());
        entry.setConsecutiveErrors(po.getConsecutiveErrors() != null ? po.getConsecutiveErrors() : 0);
        entry.setStatus(po.getStatus() != null ? po.getStatus() : "active");
        return entry;
    }

    /**
     * Get the next available API key for a provider using weighted round-robin
     *
     * @param providerType the provider type (e.g., "openai", "anthropic")
     * @return the selected key entry, or null if no keys available
     */
    public KeyEntry getNextKey(String providerType) {
        List<KeyEntry> keys = keyPool.get(providerType);
        if (keys == null || keys.isEmpty()) {
            return null;
        }

        // Filter out keys that are over quota or in error state
        List<KeyEntry> availableKeys = keys.stream()
            .filter(this::isKeyAvailable)
            .collect(Collectors.toList());

        if (availableKeys.isEmpty()) {
            log.warn("No available keys for provider: {}", providerType);
            return null;
        }

        // Use weighted round-robin selection
        return selectByWeightedRoundRobin(availableKeys);
    }

    private boolean isKeyAvailable(KeyEntry key) {
        // Check status
        if ("disabled".equals(key.getStatus()) || "error".equals(key.getStatus())) {
            return false;
        }

        // Check daily quota
        if (key.getDailyQuota() != null && key.getUsedQuotaToday() >= key.getDailyQuota()) {
            return false;
        }

        // Check consecutive errors (circuit breaker)
        if (key.getConsecutiveErrors() >= 5) {
            return false;
        }

        return true;
    }

    /**
     * Weighted round-robin selection algorithm
     */
    private KeyEntry selectByWeightedRoundRobin(List<KeyEntry> keys) {
        if (keys.size() == 1) {
            return keys.get(0);
        }

        int totalWeight = 0;
        KeyEntry selected = null;

        for (KeyEntry key : keys) {
            key.setCurrentWeight(key.getCurrentWeight() + key.getEffectiveWeight());
            totalWeight += key.getEffectiveWeight();

            if (selected == null || key.getCurrentWeight() > selected.getCurrentWeight()) {
                selected = key;
            }
        }

        if (selected != null) {
            selected.setCurrentWeight(selected.getCurrentWeight() - totalWeight);
        }

        return selected;
    }

    /**
     * Get provider configuration
     */
    public ProviderConfig getProviderConfig(String providerType) {
        return providerConfigs.get(providerType);
    }

    /**
     * Get all available provider types
     */
    public Set<String> getAvailableProviders() {
        return new HashSet<>(keyPool.keySet());
    }

    /**
     * Check if a provider has available keys
     */
    public boolean hasAvailableKeys(String providerType) {
        List<KeyEntry> keys = keyPool.get(providerType);
        if (keys == null || keys.isEmpty()) {
            return false;
        }
        return keys.stream().anyMatch(this::isKeyAvailable);
    }

    /**
     * Record successful usage of a key
     */
    public void recordSuccess(Long keyId, long tokensUsed) {
        try {
            providerApiKeyMapper.incrementUsage(keyId, tokensUsed);

            // Update local cache
            for (List<KeyEntry> keys : keyPool.values()) {
                for (KeyEntry key : keys) {
                    if (key.getId().equals(keyId)) {
                        key.setUsedQuotaToday(key.getUsedQuotaToday() + tokensUsed);
                        key.setLastUsedAt(LocalDateTime.now());
                        key.setConsecutiveErrors(0);
                        key.setStatus("active");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to record success for key: {}", keyId, e);
        }
    }

    /**
     * Record an error for a key
     */
    public void recordError(Long keyId, String errorMessage) {
        try {
            providerApiKeyMapper.recordError(keyId, errorMessage);

            // Update local cache
            for (List<KeyEntry> keys : keyPool.values()) {
                for (KeyEntry key : keys) {
                    if (key.getId().equals(keyId)) {
                        key.setConsecutiveErrors(key.getConsecutiveErrors() + 1);
                        if (key.getConsecutiveErrors() >= 5) {
                            key.setStatus("error");
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to record error for key: {}", keyId, e);
        }
    }

    /**
     * Get statistics for all keys
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        for (Map.Entry<String, List<KeyEntry>> entry : keyPool.entrySet()) {
            String provider = entry.getKey();
            List<KeyEntry> keys = entry.getValue();

            Map<String, Object> providerStats = new HashMap<>();
            providerStats.put("totalKeys", keys.size());
            providerStats.put("activeKeys", keys.stream().filter(this::isKeyAvailable).count());
            providerStats.put("keys", keys.stream().map(k -> {
                Map<String, Object> keyInfo = new HashMap<>();
                keyInfo.put("id", k.getId());
                keyInfo.put("name", k.getKeyName());
                keyInfo.put("status", k.getStatus());
                keyInfo.put("weight", k.getWeight());
                keyInfo.put("consecutiveErrors", k.getConsecutiveErrors());
                keyInfo.put("usedQuotaToday", k.getUsedQuotaToday());
                keyInfo.put("dailyQuota", k.getDailyQuota());
                return keyInfo;
            }).collect(Collectors.toList()));

            stats.put(provider, providerStats);
        }

        return stats;
    }
}
