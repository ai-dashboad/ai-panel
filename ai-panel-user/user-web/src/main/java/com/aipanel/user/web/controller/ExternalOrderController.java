package com.aipanel.user.web.controller;

import com.aipanel.common.domain.PageResult;
import com.aipanel.common.domain.R;
import com.aipanel.user.api.dto.response.DeliveryLogResponse;
import com.aipanel.user.api.dto.response.ExternalOrderResponse;
import com.aipanel.user.core.service.AutoDeliveryService;
import com.aipanel.user.core.service.ExternalOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@Tag(name = "External Order Management", description = "Admin endpoints for managing external orders")
@Slf4j
public class ExternalOrderController {

    private final ExternalOrderService externalOrderService;
    private final AutoDeliveryService autoDeliveryService;

    @GetMapping
    @Operation(summary = "List external orders with pagination")
    public R<PageResult<ExternalOrderResponse>> listOrders(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "Filter by platform") @RequestParam(required = false) String platform,
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status,
            @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword) {
        PageResult<ExternalOrderResponse> result = externalOrderService.listPage(pageNum, pageSize, platform, status, keyword);
        return R.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public R<ExternalOrderResponse> getOrder(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long id) {
        ExternalOrderResponse order = externalOrderService.getById(id);
        return R.ok(order);
    }

    @GetMapping("/{id}/logs")
    @Operation(summary = "Get delivery logs for an order")
    public R<List<DeliveryLogResponse>> getOrderLogs(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long id) {
        List<DeliveryLogResponse> logs = externalOrderService.getDeliveryLogs(id);
        return R.ok(logs);
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry delivery for a failed order")
    public R<Void> retryOrder(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long id) {
        externalOrderService.retryDelivery(id);
        return R.ok();
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order")
    public R<Void> cancelOrder(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long id) {
        externalOrderService.cancelOrder(id);
        return R.ok();
    }

    @PostMapping("/retry-failed")
    @Operation(summary = "Retry all failed orders")
    public R<Integer> retryFailedOrders(
            @Parameter(description = "Maximum retry count") @RequestParam(defaultValue = "3") Integer maxRetryCount) {
        int retried = autoDeliveryService.retryFailedOrders(maxRetryCount);
        return R.ok(retried);
    }
}
