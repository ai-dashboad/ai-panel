package com.aipanel.user.data.mysql.service;

import com.aipanel.user.data.mysql.po.ExternalOrderPO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ExternalOrderDataService extends IService<ExternalOrderPO> {

    ExternalOrderPO findByPlatformAndExternalOrderId(String platform, String externalOrderId);

    List<ExternalOrderPO> findByUserId(Long userId);

    List<ExternalOrderPO> findByStatus(String status);

    List<ExternalOrderPO> findPendingRetry(int maxRetryCount);

    IPage<ExternalOrderPO> findPage(Page<ExternalOrderPO> page, String platform, String status, String keyword);

    void updateStatus(Long id, String status, String failReason);

    void incrementRetryCount(Long id);
}
