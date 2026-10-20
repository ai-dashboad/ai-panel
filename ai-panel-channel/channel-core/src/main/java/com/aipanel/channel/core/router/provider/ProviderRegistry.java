package com.aipanel.channel.core.router.provider;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for AI providers
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderRegistry {

    private final List<AiProvider> providers;
    private final Map<String, AiProvider> providerMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        for (AiProvider provider : providers) {
            providerMap.put(provider.getName().toLowerCase(), provider);
            log.info("Registered AI provider: {} with models: {}",
                provider.getName(), provider.getSupportedModels());
        }
    }

    /**
     * Get provider by name
     */
    public AiProvider getProvider(String name) {
        return providerMap.get(name.toLowerCase());
    }

    /**
     * Get all registered providers
     */
    public Collection<AiProvider> getAllProviders() {
        return providerMap.values();
    }

    /**
     * Check if provider exists
     */
    public boolean hasProvider(String name) {
        return providerMap.containsKey(name.toLowerCase());
    }
}
