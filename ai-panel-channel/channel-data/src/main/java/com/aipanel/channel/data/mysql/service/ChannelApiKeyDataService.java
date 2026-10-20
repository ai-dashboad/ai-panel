package com.aipanel.channel.data.mysql.service;

import com.aipanel.channel.data.mysql.po.ChannelApiKeyPO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Optional;

/**
 * Channel API key data service interface
 */
public interface ChannelApiKeyDataService {

    /**
     * Create API keys in batch
     */
    List<ChannelApiKeyPO> createBatch(List<ChannelApiKeyPO> keys);

    /**
     * Find key by hash
     */
    Optional<ChannelApiKeyPO> findByKeyHash(String keyHash);

    /**
     * Find key by key ID
     */
    Optional<ChannelApiKeyPO> findByKeyId(String keyId);

    /**
     * Update key
     */
    void update(ChannelApiKeyPO key);

    /**
     * List keys by channel
     */
    IPage<ChannelApiKeyPO> listByChannel(Long channelId, Page<ChannelApiKeyPO> page);

    /**
     * Increment used quota
     */
    void incrementUsedQuota(String keyId, long tokens);

    /**
     * Count active keys for channel
     */
    long countActiveByChannel(Long channelId);

    /**
     * Count total keys for channel
     */
    long countByChannel(Long channelId);
}
