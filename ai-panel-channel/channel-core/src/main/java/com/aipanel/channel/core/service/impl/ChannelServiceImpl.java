package com.aipanel.channel.core.service.impl;

import com.aipanel.channel.api.dto.request.CreateChannelRequest;
import com.aipanel.channel.api.dto.response.ChannelResponse;
import com.aipanel.channel.core.service.ChannelService;
import com.aipanel.channel.data.mysql.po.ChannelPO;
import com.aipanel.channel.data.mysql.service.ChannelDataService;
import com.aipanel.common.domain.PageResult;
import com.aipanel.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.stream.Collectors;

/**
 * Channel service implementation
 */
@Service
@RequiredArgsConstructor
public class ChannelServiceImpl implements ChannelService {

    private final ChannelDataService channelDataService;

    @Override
    @Transactional
    public ChannelResponse create(CreateChannelRequest request) {
        // Check for duplicate name
        if (channelDataService.findByName(request.getName()).isPresent()) {
            throw new BusinessException("Channel name already exists");
        }

        ChannelPO channel = new ChannelPO();
        BeanUtils.copyProperties(request, channel);
        channel.setStatus(1); // enabled by default
        channel.setTotalKeys(0L);
        channel.setActiveKeys(0L);
        channel.setTotalRevenue(BigDecimal.ZERO);

        if (channel.getPriceMultiplier() == null) {
            channel.setPriceMultiplier(BigDecimal.ONE);
        }

        channelDataService.create(channel);
        return toResponse(channel);
    }

    @Override
    @Transactional
    public ChannelResponse update(Long channelId, CreateChannelRequest request) {
        ChannelPO channel = channelDataService.findById(channelId)
            .orElseThrow(() -> new BusinessException("Channel not found"));

        // Check for duplicate name (exclude current channel)
        channelDataService.findByName(request.getName())
            .filter(c -> !c.getId().equals(channelId))
            .ifPresent(c -> {
                throw new BusinessException("Channel name already exists");
            });

        channel.setName(request.getName());
        channel.setType(request.getType());
        channel.setDescription(request.getDescription());
        channel.setKeyPrefix(request.getKeyPrefix());
        channel.setPriceMultiplier(request.getPriceMultiplier());
        channel.setWebhookUrl(request.getWebhookUrl());
        channel.setConfig(request.getConfig());

        channelDataService.update(channel);
        return toResponse(channel);
    }

    @Override
    @Transactional
    public void delete(Long channelId) {
        channelDataService.findById(channelId)
            .orElseThrow(() -> new BusinessException("Channel not found"));
        channelDataService.delete(channelId);
    }

    @Override
    public ChannelResponse getById(Long channelId) {
        ChannelPO channel = channelDataService.findById(channelId)
            .orElseThrow(() -> new BusinessException("Channel not found"));
        return toResponse(channel);
    }

    @Override
    public PageResult<ChannelResponse> list(int pageNum, int pageSize) {
        Page<ChannelPO> page = new Page<>(pageNum, pageSize);
        IPage<ChannelPO> result = channelDataService.list(page);

        PageResult<ChannelResponse> pageResult = new PageResult<>();
        pageResult.setList(result.getRecords().stream()
            .map(this::toResponse)
            .collect(Collectors.toList()));
        pageResult.setTotal(result.getTotal());
        pageResult.setPageNum((int) result.getCurrent());
        pageResult.setPageSize((int) result.getSize());
        return pageResult;
    }

    @Override
    @Transactional
    public void enable(Long channelId) {
        ChannelPO channel = channelDataService.findById(channelId)
            .orElseThrow(() -> new BusinessException("Channel not found"));
        channel.setStatus(1);
        channelDataService.update(channel);
    }

    @Override
    @Transactional
    public void disable(Long channelId) {
        ChannelPO channel = channelDataService.findById(channelId)
            .orElseThrow(() -> new BusinessException("Channel not found"));
        channel.setStatus(0);
        channelDataService.update(channel);
    }

    private ChannelResponse toResponse(ChannelPO channel) {
        ChannelResponse response = new ChannelResponse();
        BeanUtils.copyProperties(channel, response);
        return response;
    }
}
