package com.aipanel.billing.core.service.impl;

import com.aipanel.billing.api.dto.request.RecordUsageRequest;
import com.aipanel.billing.api.dto.response.BillingRecordResponse;
import com.aipanel.billing.api.dto.response.UsageSummaryResponse;
import com.aipanel.billing.core.service.BillingService;
import com.aipanel.billing.data.po.UsageRecordPO;
import com.aipanel.billing.data.service.UsageDataService;
import com.aipanel.common.domain.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Billing Service Implementation
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private final UsageDataService usageDataService;

    private static final BigDecimal DEFAULT_INPUT_PRICE = new BigDecimal("0.01");
    private static final BigDecimal DEFAULT_OUTPUT_PRICE = new BigDecimal("0.03");

    @Override
    public void recordUsage(RecordUsageRequest request) {
        log.info("Recording usage: userId={}, model={}, tokens={}",
            request.getUserId(), request.getModel(), request.getInputTokens() + request.getOutputTokens());

        BigDecimal inputCost = calculateCost(request.getInputTokens(), DEFAULT_INPUT_PRICE);
        BigDecimal outputCost = calculateCost(request.getOutputTokens(), DEFAULT_OUTPUT_PRICE);
        BigDecimal totalCost = inputCost.add(outputCost);

        UsageRecordPO record = new UsageRecordPO();
        record.setUserId(request.getUserId());
        record.setApiKeyId(request.getApiKeyId());
        record.setRequestId(request.getRequestId() != null ? request.getRequestId() : UUID.randomUUID().toString());
        record.setModel(request.getModel());
        record.setProvider(request.getProvider());
        record.setInputTokens(request.getInputTokens());
        record.setOutputTokens(request.getOutputTokens());
        record.setTotalTokens(request.getInputTokens() + request.getOutputTokens());
        record.setInputCost(inputCost);
        record.setOutputCost(outputCost);
        record.setTotalCost(totalCost);
        record.setLatencyMs(request.getLatencyMs());
        record.setStatus(request.getStatus() != null ? request.getStatus() : "success");
        record.setCreateTime(LocalDateTime.now());

        usageDataService.save(record);
        log.info("Usage recorded: cost={}", totalCost);
    }

    @Override
    public UsageSummaryResponse getUsageSummary(Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("Getting usage summary: userId={}, startDate={}, endDate={}", userId, startDate, endDate);

        Map<String, Object> stats = usageDataService.getSummaryStats(userId, startDate, endDate);
        List<Map<String, Object>> modelStats = usageDataService.getModelStats(userId, startDate, endDate);
        List<Map<String, Object>> dailyStats = usageDataService.getDailyStats(userId, startDate, endDate);

        // Convert model stats
        java.util.Map<String, UsageSummaryResponse.ModelUsage> usageByModel = new java.util.HashMap<>();
        for (Map<String, Object> ms : modelStats) {
            String model = (String) ms.get("model");
            usageByModel.put(model, UsageSummaryResponse.ModelUsage.builder()
                .requestCount(toLong(ms.get("request_count")))
                .inputTokens(toLong(ms.get("input_tokens")))
                .outputTokens(toLong(ms.get("output_tokens")))
                .cost(toBigDecimal(ms.get("total_cost")))
                .build());
        }

        // Convert daily stats
        List<UsageSummaryResponse.DailyUsage> usageByDay = dailyStats.stream()
            .map(ds -> UsageSummaryResponse.DailyUsage.builder()
                .date(String.valueOf(ds.get("date")))
                .requestCount(toLong(ds.get("request_count")))
                .totalTokens(toLong(ds.get("total_tokens")))
                .cost(toBigDecimal(ds.get("total_cost")))
                .build())
            .collect(Collectors.toList());

        return UsageSummaryResponse.builder()
            .userId(userId)
            .totalRequests(toLong(stats.get("total_requests")))
            .successfulRequests(toLong(stats.get("successful_requests")))
            .failedRequests(toLong(stats.get("failed_requests")))
            .totalInputTokens(toLong(stats.get("total_input_tokens")))
            .totalOutputTokens(toLong(stats.get("total_output_tokens")))
            .totalTokens(toLong(stats.get("total_tokens")))
            .totalCost(toBigDecimal(stats.get("total_cost")))
            .averageLatencyMs(toDouble(stats.get("avg_latency")))
            .usageByModel(usageByModel)
            .usageByDay(usageByDay)
            .build();
    }

    @Override
    public PageResult<BillingRecordResponse> getBillingRecords(Long userId, int page, int size) {
        log.info("Getting billing records: userId={}, page={}, size={}", userId, page, size);

        Page<UsageRecordPO> pageResult = usageDataService.getUsageRecords(userId, page, size);

        List<BillingRecordResponse> records = pageResult.getRecords().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return PageResult.of(records, (int) pageResult.getCurrent(), (int) pageResult.getSize(), pageResult.getTotal());
    }

    @Override
    public BigDecimal getBalance(Long userId) {
        log.info("Getting balance: userId={}", userId);
        return BigDecimal.ZERO;
    }

    @Override
    public void addCredits(Long userId, BigDecimal amount, String description) {
        log.info("Adding credits: userId={}, amount={}, description={}", userId, amount, description);
    }

    @Override
    public boolean deductBalance(Long userId, BigDecimal amount) {
        log.info("Deducting balance: userId={}, amount={}", userId, amount);
        return true;
    }

    private BigDecimal calculateCost(Integer tokens, BigDecimal pricePerThousand) {
        if (tokens == null || tokens <= 0) {
            return BigDecimal.ZERO;
        }
        return pricePerThousand.multiply(BigDecimal.valueOf(tokens))
            .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
    }

    private BillingRecordResponse toResponse(UsageRecordPO po) {
        return BillingRecordResponse.builder()
            .id(po.getId())
            .requestId(po.getRequestId())
            .model(po.getModel())
            .provider(po.getProvider())
            .inputTokens(po.getInputTokens())
            .outputTokens(po.getOutputTokens())
            .totalTokens(po.getTotalTokens())
            .inputCost(po.getInputCost())
            .outputCost(po.getOutputCost())
            .totalCost(po.getTotalCost())
            .latencyMs(po.getLatencyMs())
            .status(po.getStatus())
            .createTime(po.getCreateTime())
            .build();
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        return BigDecimal.ZERO;
    }

    private Double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return 0.0;
    }
}
