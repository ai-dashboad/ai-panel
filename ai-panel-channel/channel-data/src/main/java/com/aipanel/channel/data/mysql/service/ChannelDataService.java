package com.aipanel.channel.data.mysql.service;

import com.aipanel.channel.data.mysql.po.ChannelPO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Optional;

/**
 * Channel data service interface
 */
public interface ChannelDataService {

    /**
     * Create a channel
     */
    ChannelPO create(ChannelPO channel);

    /**
     * Update a channel
     */
    void update(ChannelPO channel);

    /**
     * Delete a channel (soft delete)
     */
    void delete(Long id);

    /**
     * Find channel by ID
     */
    Optional<ChannelPO> findById(Long id);

    /**
     * Find channel by name
     */
    Optional<ChannelPO> findByName(String name);

    /**
     * List channels with pagination
     */
    IPage<ChannelPO> list(Page<ChannelPO> page);

    /**
     * Update channel key statistics
     */
    void updateKeyStats(Long channelId, long totalKeys, long activeKeys);

    /**
     * Increment total revenue
     */
    void incrementRevenue(Long channelId, java.math.BigDecimal amount);
}
