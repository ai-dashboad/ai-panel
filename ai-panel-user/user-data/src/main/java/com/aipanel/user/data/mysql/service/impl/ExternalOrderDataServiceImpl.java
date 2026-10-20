package com.aipanel.user.data.mysql.service.impl;

import com.aipanel.user.data.mysql.mapper.ExternalOrderMapper;
import com.aipanel.user.data.mysql.po.ExternalOrderPO;
import com.aipanel.user.data.mysql.service.ExternalOrderDataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalOrderDataServiceImpl extends ServiceImpl<ExternalOrderMapper, ExternalOrderPO> implements ExternalOrderDataService {

    @Override
    public ExternalOrderPO findByPlatformAndExternalOrderId(String platform, String externalOrderId) {
        return getOne(new LambdaQueryWrapper<ExternalOrderPO>()
                .eq(ExternalOrderPO::getPlatform, platform)
                .eq(ExternalOrderPO::getExternalOrderId, externalOrderId));
    }

    @Override
    public List<ExternalOrderPO> findByUserId(Long userId) {
        return list(new LambdaQueryWrapper<ExternalOrderPO>()
                .eq(ExternalOrderPO::getUserId, userId)
                .orderByDesc(ExternalOrderPO::getCreatedAt));
    }

    @Override
    public List<ExternalOrderPO> findByStatus(String status) {
        return list(new LambdaQueryWrapper<ExternalOrderPO>()
                .eq(ExternalOrderPO::getStatus, status)
                .orderByAsc(ExternalOrderPO::getCreatedAt));
    }

    @Override
    public List<ExternalOrderPO> findPendingRetry(int maxRetryCount) {
        return list(new LambdaQueryWrapper<ExternalOrderPO>()
                .eq(ExternalOrderPO::getStatus, ExternalOrderPO.STATUS_FAILED)
                .lt(ExternalOrderPO::getRetryCount, maxRetryCount)
                .orderByAsc(ExternalOrderPO::getCreatedAt)
                .last("LIMIT 100"));
    }

    @Override
    public IPage<ExternalOrderPO> findPage(Page<ExternalOrderPO> page, String platform, String status, String keyword) {
        LambdaQueryWrapper<ExternalOrderPO> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(platform)) {
            wrapper.eq(ExternalOrderPO::getPlatform, platform);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(ExternalOrderPO::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(ExternalOrderPO::getExternalOrderId, keyword)
                    .or()
                    .like(ExternalOrderPO::getBuyerNick, keyword)
                    .or()
                    .like(ExternalOrderPO::getBuyerPhone, keyword)
                    .or()
                    .like(ExternalOrderPO::getBuyerEmail, keyword));
        }

        wrapper.orderByDesc(ExternalOrderPO::getCreatedAt);
        return page(page, wrapper);
    }

    @Override
    public void updateStatus(Long id, String status, String failReason) {
        update(new LambdaUpdateWrapper<ExternalOrderPO>()
                .eq(ExternalOrderPO::getId, id)
                .set(ExternalOrderPO::getStatus, status)
                .set(ExternalOrderPO::getFailReason, failReason)
                .set(ExternalOrderPO::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    public void incrementRetryCount(Long id) {
        update(new LambdaUpdateWrapper<ExternalOrderPO>()
                .eq(ExternalOrderPO::getId, id)
                .setSql("retry_count = retry_count + 1")
                .set(ExternalOrderPO::getUpdatedAt, LocalDateTime.now()));
    }
}
