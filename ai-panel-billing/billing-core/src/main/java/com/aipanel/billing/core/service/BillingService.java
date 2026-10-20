package com.aipanel.billing.core.service;

import com.aipanel.billing.api.dto.request.RecordUsageRequest;
import com.aipanel.billing.api.dto.response.BillingRecordResponse;
import com.aipanel.billing.api.dto.response.UsageSummaryResponse;
import com.aipanel.common.domain.PageResult;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Billing Service Interface
 */
public interface BillingService {

    /**
     * Record API usage
     */
    void recordUsage(RecordUsageRequest request);

    /**
     * Get usage summary for a user
     */
    UsageSummaryResponse getUsageSummary(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Get billing records for a user
     */
    PageResult<BillingRecordResponse> getBillingRecords(Long userId, int page, int size);

    /**
     * Get user balance
     */
    BigDecimal getBalance(Long userId);

    /**
     * Add credits to user balance
     */
    void addCredits(Long userId, BigDecimal amount, String description);

    /**
     * Deduct from user balance
     */
    boolean deductBalance(Long userId, BigDecimal amount);
}
