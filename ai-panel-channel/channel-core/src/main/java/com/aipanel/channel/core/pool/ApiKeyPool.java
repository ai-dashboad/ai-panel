package com.aipanel.channel.core.pool;

import com.aipanel.channel.data.mysql.mapper.ProviderApiKeyMapper;
import com.aipanel.channel.data.mysql.mapper.ProviderConfigMapper;
import com.aipanel.channel.data.mysql.mapper.ProviderModelMapper;
import com.aipanel.channel.data.mysql.po.ProviderModelPO;
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
    private final ProviderModelMapper providerModelMapper;

    /**
     * Cache: providerId -> list of active keys. Several providers may share a type
     * (e.g. two vLLM servers), so everything is keyed by provider id.
     */
    private volatile Map<Long, List<KeyEntry>> keyPool = new ConcurrentHashMap<>();

    /**
     * Provider configuration cache: providerId -> config (enabled providers only)
     */
    private volatile Map<Long, ProviderConfig> providerConfigs = new ConcurrentHashMap<>();

    /**
     * Legacy view: providerType -> highest-priority enabled provider of that type
     */
    private volatile Map<String, ProviderConfig> primaryByType = new ConcurrentHashMap<>();

    /**
     * Enabled models: modelId -> provider ids (enabled providers only)
     */
    private volatile Map<String, List<Long>> modelIndex = new ConcurrentHashMap<>();

    /**
     * Enabled models per provider: providerId -> model ids
     */
    private volatile Map<Long, List<String>> modelsByProvider = new ConcurrentHashMap<>();

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
    public synchronized void reload() {
        try {
            List<ProviderConfigPO> providers = providerConfigMapper.selectList(
                new LambdaQueryWrapper<ProviderConfigPO>()
                    .eq(ProviderConfigPO::getEnabled, true)
            );

            Map<Long, ProviderConfig> newConfigs = new ConcurrentHashMap<>();
            Map<String, ProviderConfig> newPrimary = new ConcurrentHashMap<>();
            for (ProviderConfigPO po : providers) {
                ProviderConfig config = new ProviderConfig();
                config.setId(po.getId());
                config.setName(po.getName());
                config.setType(po.getType());
                config.setBaseUrl(po.getBaseUrl());
                config.setTimeoutSeconds(po.getTimeoutSeconds() != null ? po.getTimeoutSeconds() : 60);
                config.setRetryCount(po.getRetryCount() != null ? po.getRetryCount() : 3);
                config.setPriority(po.getPriority() != null ? po.getPriority() : 100);
                newConfigs.put(po.getId(), config);
                newPrimary.merge(po.getType(), config,
                    (a, b) -> a.getPriority() <= b.getPriority() ? a : b);
            }

            Map<Long, List<KeyEntry>> newPool = new ConcurrentHashMap<>();
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
                    newPool.put(provider.getId(), entries);
                    log.info("Loaded {} keys for provider: {} ({})", entries.size(), provider.getName(), provider.getType());
                }
            }

            Map<String, List<Long>> newModelIndex = new ConcurrentHashMap<>();
            Map<Long, List<String>> newModelsByProvider = new ConcurrentHashMap<>();
            if (!newConfigs.isEmpty()) {
                List<ProviderModelPO> models = providerModelMapper.selectList(
                    new LambdaQueryWrapper<ProviderModelPO>()
                        .eq(ProviderModelPO::getEnabled, true)
                        .in(ProviderModelPO::getProviderId, newConfigs.keySet())
                );
                for (ProviderModelPO model : models) {
                    newModelIndex.computeIfAbsent(model.getModelId(), k -> new ArrayList<>()).add(model.getProviderId());
                    newModelsByProvider.computeIfAbsent(model.getProviderId(), k -> new ArrayList<>()).add(model.getModelId());
                }
            }

            providerConfigs = newConfigs;
            primaryByType = newPrimary;
            keyPool = newPool;
            modelIndex = newModelIndex;
            modelsByProvider = newModelsByProvider;

            log.info("API Key Pool reloaded: {} enabled providers, {} with keys", newConfigs.size(), newPool.size());

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
     * @param providerId the provider_config id
     * @return the selected key entry, or null if no keys available
     */
    public KeyEntry getNextKey(Long providerId) {
        List<KeyEntry> keys = providerId == null ? null : keyPool.get(providerId);
        if (keys == null || keys.isEmpty()) {
            return null;
        }

        // Filter out keys that are over quota or in error state
        List<KeyEntry> availableKeys = keys.stream()
            .filter(this::isKeyAvailable)
            .collect(Collectors.toList());

        if (availableKeys.isEmpty()) {
            log.warn("No available keys for provider id: {}", providerId);
            return null;
        }

        synchronized (keys) {
            return selectByWeightedRoundRobin(availableKeys);
        }
    }

    /**
     * Legacy lookup by provider type (uses the highest-priority provider of that type).
     */
    public KeyEntry getNextKey(String providerType) {
        ProviderConfig config = primaryByType.get(providerType);
        return config == null ? null : getNextKey(config.getId());
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
     * Get provider configuration by provider id (enabled providers only)
     */
    public ProviderConfig getProviderConfig(Long providerId) {
        return providerId == null ? null : providerConfigs.get(providerId);
    }

    /**
     * Legacy: highest-priority enabled provider of the given type
     */
    public ProviderConfig getProviderConfig(String providerType) {
        return primaryByType.get(providerType);
    }

    /**
     * All enabled providers, ordered by priority (lower first) then id
     */
    public List<ProviderConfig> getEnabledProviders() {
        return providerConfigs.values().stream()
            .sorted(Comparator.comparingInt(ProviderConfig::getPriority).thenComparing(ProviderConfig::getId))
            .collect(Collectors.toList());
    }

    /**
     * Enabled providers of the given type, ordered by priority
     */
    public List<ProviderConfig> getEnabledProvidersByType(String providerType) {
        return getEnabledProviders().stream()
            .filter(p -> providerType.equalsIgnoreCase(p.getType()))
            .collect(Collectors.toList());
    }

    /**
     * Enabled providers that explicitly list the given model (exact match first, then
     * case-insensitive), ordered by provider priority.
     */
    public List<ProviderConfig> getProvidersForModel(String modelId) {
        if (modelId == null) {
            return List.of();
        }
        List<Long> ids = modelIndex.get(modelId);
        if (ids == null) {
            ids = modelIndex.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(modelId))
                .flatMap(e -> e.getValue().stream())
                .collect(Collectors.toList());
        }
        Set<Long> idSet = new HashSet<>(ids);
        return getEnabledProviders().stream()
            .filter(p -> idSet.contains(p.getId()))
            .collect(Collectors.toList());
    }

    /**
     * Enabled model ids of a provider
     */
    public List<String> getModelsForProvider(Long providerId) {
        return modelsByProvider.getOrDefault(providerId, List.of());
    }

    /**
     * Number of loaded keys for a provider
     */
    public int getKeyCount(Long providerId) {
        List<KeyEntry> keys = providerId == null ? null : keyPool.get(providerId);
        return keys == null ? 0 : keys.size();
    }

    /**
     * Types of all enabled providers that have at least one key loaded
     */
    public Set<String> getAvailableProviders() {
        Set<String> types = new HashSet<>();
        keyPool.keySet().forEach(id -> {
            ProviderConfig c = providerConfigs.get(id);
            if (c != null) {
                types.add(c.getType());
            }
        });
        return types;
    }

    /**
     * Check if a provider has available keys
     */
    public boolean hasAvailableKeys(Long providerId) {
        List<KeyEntry> keys = providerId == null ? null : keyPool.get(providerId);
        if (keys == null || keys.isEmpty()) {
            return false;
        }
        return keys.stream().anyMatch(this::isKeyAvailable);
    }

    /**
     * Legacy: whether the highest-priority provider of this type has available keys
     */
    public boolean hasAvailableKeys(String providerType) {
        ProviderConfig config = primaryByType.get(providerType);
        return config != null && hasAvailableKeys(config.getId());
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

        for (Map.Entry<Long, List<KeyEntry>> entry : keyPool.entrySet()) {
            ProviderConfig config = providerConfigs.get(entry.getKey());
            if (config == null) {
                continue;
            }
            String provider = stats.containsKey(config.getType())
                ? config.getType() + ":" + config.getName() : config.getType();
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
