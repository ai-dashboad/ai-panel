package com.aipanel.key.data.mysql.service.impl;

import com.aipanel.key.data.mysql.mapper.ApiKeyMapper;
import com.aipanel.key.data.mysql.po.ApiKeyPO;
import com.aipanel.key.data.mysql.service.ApiKeyDataService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * API Key data service implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyDataServiceImpl extends ServiceImpl<ApiKeyMapper, ApiKeyPO> implements ApiKeyDataService {

    private final ApiKeyMapper apiKeyMapper;

    @Override
    public ApiKeyPO findByKeyId(String keyId) {
        return apiKeyMapper.findByKeyId(keyId);
    }

    @Override
    public ApiKeyPO findByKeyHash(String keyHash) {
        return apiKeyMapper.findByKeyHash(keyHash);
    }

    @Override
    public ApiKeyPO findByKeyIdAndUserId(String keyId, Long userId) {
        return apiKeyMapper.findByKeyIdAndUserId(keyId, userId);
    }

    @Override
    public IPage<ApiKeyPO> findPageByUserId(Page<ApiKeyPO> page, Long userId) {
        return apiKeyMapper.findPageByUserId(page, userId);
    }

    @Override
    public long countByUserId(Long userId) {
        return apiKeyMapper.countByUserId(userId);
    }

    @Override
    public void updateLastUsed(String keyId, LocalDateTime lastUsedAt) {
        apiKeyMapper.updateLastUsed(keyId, lastUsedAt);
    }
}
