package com.aipanel.billing.api;

import com.aipanel.billing.api.dto.request.RecordUsageRequest;
import com.aipanel.billing.api.dto.response.UsageSummaryResponse;
import com.aipanel.billing.api.dto.response.BillingRecordResponse;
import com.aipanel.common.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Billing Service Feign API
 */
@FeignClient(name = "billing-service", path = "/api/v1/billing")
public interface BillingApi {

    @PostMapping("/usage")
    R<Void> recordUsage(@RequestBody RecordUsageRequest request);

    @GetMapping("/usage/summary")
    R<UsageSummaryResponse> getUsageSummary(
        @RequestParam Long userId,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate
    );

    @GetMapping("/records")
    R<List<BillingRecordResponse>> getBillingRecords(
        @RequestParam Long userId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    );

    @GetMapping("/balance")
    R<java.math.BigDecimal> getBalance(@RequestParam Long userId);
}
