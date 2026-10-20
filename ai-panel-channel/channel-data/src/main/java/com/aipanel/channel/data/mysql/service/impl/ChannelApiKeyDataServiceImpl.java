package com.aipanel.channel.data.mysql.service.impl;

import com.aipanel.channel.data.mysql.mapper.ChannelApiKeyMapper;
import com.aipanel.channel.data.mysql.po.ChannelApiKeyPO;
import com.aipanel.channel.data.mysql.service.ChannelApiKeyDataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Channel API key data service implementation
 */
@Service
@RequiredArgsConstructor
public class ChannelApiKeyDataServiceImpl implements ChannelApiKeyDataService {

    private final ChannelApiKeyMapper keyMapper;

    @Override
    @Transactional
    public List<ChannelApiKeyPO> createBatch(List<ChannelApiKeyPO> keys) {
        for (ChannelApiKeyPO key : keys) {
            keyMapper.insert(key);
        }
        return keys;
    }

    @Override
    public Optional<ChannelApiKeyPO> findByKeyHash(String keyHash) {
        return Optional.ofNullable(keyMapper.findByKeyHash(keyHash));
    }

    @Override
    public Optional<ChannelApiKeyPO> findByKeyId(String keyId) {
        return Optional.ofNullable(keyMapper.findByKeyId(keyId));
    }

    @Override
    public void update(ChannelApiKeyPO key) {
        keyMapper.updateById(key);
    }

    @Override
    public IPage<ChannelApiKeyPO> listByChannel(Long channelId, Page<ChannelApiKeyPO> page) {
        LambdaQueryWrapper<ChannelApiKeyPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChannelApiKeyPO::getChannelId, channelId)
               .orderByDesc(ChannelApiKeyPO::getCreatedAt);
        return keyMapper.selectPage(page, wrapper);
    }

    @Override
    public void incrementUsedQuota(String keyId, long tokens) {
        LambdaUpdateWrapper<ChannelApiKeyPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChannelApiKeyPO::getKeyId, keyId)
               .setSql("used_quota = used_quota + " + tokens)
               .set(ChannelApiKeyPO::getLastUsedAt, LocalDateTime.now());
        keyMapper.update(null, wrapper);
    }

    @Override
    public long countActiveByChannel(Long channelId) {
        LambdaQueryWrapper<ChannelApiKeyPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChannelApiKeyPO::getChannelId, channelId)
               .eq(ChannelApiKeyPO::getStatus, 1);
        return keyMapper.selectCount(wrapper);
    }

    @Override
    public long countByChannel(Long channelId) {
        LambdaQueryWrapper<ChannelApiKeyPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChannelApiKeyPO::getChannelId, channelId);
        return keyMapper.selectCount(wrapper);
    }
}
