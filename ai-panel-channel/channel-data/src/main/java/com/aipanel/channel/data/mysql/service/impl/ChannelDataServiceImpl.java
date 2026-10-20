package com.aipanel.channel.data.mysql.service.impl;

import com.aipanel.channel.data.mysql.mapper.ChannelMapper;
import com.aipanel.channel.data.mysql.po.ChannelPO;
import com.aipanel.channel.data.mysql.service.ChannelDataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Channel data service implementation
 */
@Service
@RequiredArgsConstructor
public class ChannelDataServiceImpl implements ChannelDataService {

    private final ChannelMapper channelMapper;

    @Override
    public ChannelPO create(ChannelPO channel) {
        channelMapper.insert(channel);
        return channel;
    }

    @Override
    public void update(ChannelPO channel) {
        channelMapper.updateById(channel);
    }

    @Override
    public void delete(Long id) {
        channelMapper.deleteById(id);
    }

    @Override
    public Optional<ChannelPO> findById(Long id) {
        return Optional.ofNullable(channelMapper.selectById(id));
    }

    @Override
    public Optional<ChannelPO> findByName(String name) {
        LambdaQueryWrapper<ChannelPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChannelPO::getName, name);
        return Optional.ofNullable(channelMapper.selectOne(wrapper));
    }

    @Override
    public IPage<ChannelPO> list(Page<ChannelPO> page) {
        LambdaQueryWrapper<ChannelPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ChannelPO::getCreatedAt);
        return channelMapper.selectPage(page, wrapper);
    }

    @Override
    public void updateKeyStats(Long channelId, long totalKeys, long activeKeys) {
        LambdaUpdateWrapper<ChannelPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChannelPO::getId, channelId)
               .set(ChannelPO::getTotalKeys, totalKeys)
               .set(ChannelPO::getActiveKeys, activeKeys);
        channelMapper.update(null, wrapper);
    }

    @Override
    public void incrementRevenue(Long channelId, BigDecimal amount) {
        ChannelPO channel = channelMapper.selectById(channelId);
        if (channel != null) {
            BigDecimal currentRevenue = channel.getTotalRevenue() != null
                ? channel.getTotalRevenue() : BigDecimal.ZERO;
            LambdaUpdateWrapper<ChannelPO> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(ChannelPO::getId, channelId)
                   .set(ChannelPO::getTotalRevenue, currentRevenue.add(amount));
            channelMapper.update(null, wrapper);
        }
    }
}
