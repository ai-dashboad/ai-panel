package com.aipanel.user.web.controller;

import com.aipanel.common.domain.R;
import com.aipanel.user.api.dto.request.WebhookRequest;
import com.aipanel.user.api.dto.response.ExternalOrderResponse;
import com.aipanel.user.core.service.AutoDeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
@Tag(name = "Webhook", description = "External platform webhook callbacks")
@Slf4j
public class WebhookController {

    private final AutoDeliveryService autoDeliveryService;

    @PostMapping("/{platform}")
    @Operation(summary = "Process webhook callback from external platform")
    public R<ExternalOrderResponse> handleWebhook(
            @Parameter(description = "Platform identifier", required = true)
            @PathVariable String platform,
            @RequestBody WebhookRequest request,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestHeader(value = "X-Timestamp", required = false) String timestamp) {

        log.info("Webhook received: platform={}, orderId={}", platform, request.getOrderId());

        try {
            // Build raw body for signature verification if needed
            String rawBody = request.getRawBody();

            ExternalOrderResponse response = autoDeliveryService.processWebhook(
                    platform, request, signature, rawBody);

            return R.ok(response);
        } catch (Exception e) {
            log.error("Webhook processing failed: platform={}, error={}", platform, e.getMessage(), e);
            return R.fail(500, "Webhook processing failed: " + e.getMessage());
        }
    }

    @GetMapping("/{platform}/test")
    @Operation(summary = "Test webhook endpoint (for platform verification)")
    public R<String> testWebhook(
            @PathVariable String platform,
            @RequestParam(required = false) String challenge) {

        log.info("Webhook test received: platform={}, challenge={}", platform, challenge);

        // Return challenge for verification (some platforms require this)
        if (challenge != null) {
            return R.ok(challenge);
        }

        return R.ok("Webhook endpoint is active for platform: " + platform);
    }
}
