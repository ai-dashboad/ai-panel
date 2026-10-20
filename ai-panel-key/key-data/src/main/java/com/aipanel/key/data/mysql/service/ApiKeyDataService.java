package com.aipanel.key.data.mysql.service;

import com.aipanel.key.data.mysql.po.ApiKeyPO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;

/**
 * API Key data service interface
 */
public interface ApiKeyDataService extends IService<ApiKeyPO> {

    ApiKeyPO findByKeyId(String keyId);

    ApiKeyPO findByKeyHash(String keyHash);

    ApiKeyPO findByKeyIdAndUserId(String keyId, Long userId);

    IPage<ApiKeyPO> findPageByUserId(Page<ApiKeyPO> page, Long userId);

    long countByUserId(Long userId);

    void updateLastUsed(String keyId, LocalDateTime lastUsedAt);
}
