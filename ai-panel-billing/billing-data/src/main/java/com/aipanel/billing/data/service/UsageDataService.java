package com.aipanel.billing.data.service;

import com.aipanel.billing.data.mapper.UsageRecordMapper;
import com.aipanel.billing.data.po.UsageRecordPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UsageDataService {

    private final UsageRecordMapper usageRecordMapper;

    public void save(UsageRecordPO record) {
        usageRecordMapper.insert(record);
    }

    public Map<String, Object> getSummaryStats(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);
        return usageRecordMapper.getSummaryStats(userId, startTime, endTime);
    }

    public List<Map<String, Object>> getDailyStats(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);
        return usageRecordMapper.getDailyStats(userId, startTime, endTime);
    }

    public List<Map<String, Object>> getModelStats(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);
        return usageRecordMapper.getModelStats(userId, startTime, endTime);
    }

    public Page<UsageRecordPO> getUsageRecords(Long userId, int page, int size) {
        Page<UsageRecordPO> pageRequest = new Page<>(page, size);
        LambdaQueryWrapper<UsageRecordPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UsageRecordPO::getUserId, userId)
               .orderByDesc(UsageRecordPO::getCreateTime);
        return usageRecordMapper.selectPage(pageRequest, wrapper);
    }
}
