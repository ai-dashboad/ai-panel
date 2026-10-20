package com.aipanel.user.core.service;

import com.aipanel.user.api.dto.request.WebhookRequest;
import com.aipanel.user.api.dto.response.ExternalOrderResponse;

public interface AutoDeliveryService {

    /**
     * Process webhook callback from external platform
     *
     * @param platform Platform identifier
     * @param request Webhook request data
     * @param signature Request signature for verification
     * @param rawBody Raw request body for signature verification
     * @return Processed order response
     */
    ExternalOrderResponse processWebhook(String platform, WebhookRequest request, String signature, String rawBody);

    /**
     * Process a single order delivery
     *
     * @param orderId Order ID
     * @return Updated order response
     */
    ExternalOrderResponse processOrder(Long orderId);

    /**
     * Retry failed orders
     *
     * @param maxRetryCount Maximum retry attempts
     * @return Number of orders retried
     */
    int retryFailedOrders(int maxRetryCount);

    /**
     * Verify webhook signature
     *
     * @param platform Platform identifier
     * @param signature Signature from request
     * @param rawBody Raw request body
     * @return true if signature is valid
     */
    boolean verifySignature(String platform, String signature, String rawBody);
}
