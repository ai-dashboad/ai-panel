package com.aipanel.channel.core.service;

import com.aipanel.channel.api.dto.request.CreateChannelRequest;
import com.aipanel.channel.api.dto.response.ChannelResponse;
import com.aipanel.common.domain.PageResult;

/**
 * Channel service interface
 */
public interface ChannelService {

    /**
     * Create a new channel
     */
    ChannelResponse create(CreateChannelRequest request);

    /**
     * Update channel
     */
    ChannelResponse update(Long channelId, CreateChannelRequest request);

    /**
     * Delete channel
     */
    void delete(Long channelId);

    /**
     * Get channel by ID
     */
    ChannelResponse getById(Long channelId);

    /**
     * List channels with pagination
     */
    PageResult<ChannelResponse> list(int pageNum, int pageSize);

    /**
     * Enable channel
     */
    void enable(Long channelId);

    /**
     * Disable channel
     */
    void disable(Long channelId);
}
