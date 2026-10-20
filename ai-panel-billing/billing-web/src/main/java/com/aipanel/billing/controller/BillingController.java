package com.aipanel.billing.controller;

import com.aipanel.billing.api.dto.request.RecordUsageRequest;
import com.aipanel.billing.api.dto.response.BillingRecordResponse;
import com.aipanel.billing.api.dto.response.UsageSummaryResponse;
import com.aipanel.billing.core.service.BillingService;
import com.aipanel.common.domain.PageResult;
import com.aipanel.common.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Billing Controller
 */
@RestController
@RequestMapping("/api/v1/billing")
@Tag(name = "Billing", description = "Billing and Usage API")
@Slf4j
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/usage")
    @Operation(summary = "Record usage", description = "Record API usage for billing")
    public R<Void> recordUsage(@Valid @RequestBody RecordUsageRequest request) {
        billingService.recordUsage(request);
        return R.ok();
    }

    @GetMapping("/usage/summary")
    @Operation(summary = "Get usage summary", description = "Get usage summary for a user")
    public R<UsageSummaryResponse> getUsageSummary(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Start date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        return R.ok(billingService.getUsageSummary(userId, startDate, endDate));
    }

    @GetMapping("/records")
    @Operation(summary = "Get billing records", description = "Get billing records for a user")
    public R<PageResult<BillingRecordResponse>> getBillingRecords(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size
    ) {
        return R.ok(billingService.getBillingRecords(userId, page, size));
    }

    @GetMapping("/balance")
    @Operation(summary = "Get balance", description = "Get user balance")
    public R<BigDecimal> getBalance(
            @Parameter(description = "User ID") @RequestParam Long userId
    ) {
        return R.ok(billingService.getBalance(userId));
    }

    @PostMapping("/credits")
    @Operation(summary = "Add credits", description = "Add credits to user balance")
    public R<Void> addCredits(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Amount") @RequestParam BigDecimal amount,
            @Parameter(description = "Description") @RequestParam(required = false) String description
    ) {
        billingService.addCredits(userId, amount, description);
        return R.ok();
    }
}
