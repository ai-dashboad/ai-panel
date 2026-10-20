package com.aipanel.channel.core.service;

import com.aipanel.channel.api.dto.request.GenerateApiKeysRequest;
import com.aipanel.channel.api.dto.response.GeneratedKeyResponse;
import com.aipanel.channel.data.mysql.po.ChannelApiKeyPO;

import java.util.List;

/**
 * API key generator service
 */
public interface ApiKeyGeneratorService {

    /**
     * Generate batch of API keys
     */
    List<GeneratedKeyResponse> generateKeys(GenerateApiKeysRequest request);

    /**
     * Validate API key and return key data if valid
     */
    ChannelApiKeyPO validateKey(String apiKey);

    /**
     * Check if key has sufficient quota
     */
    boolean hasQuota(String keyId, int tokensNeeded);

    /**
     * Consume quota after successful API call
     */
    void consumeQuota(String keyId, int tokens);
}
