package com.aipanel.user.core.service.impl;

import com.aipanel.common.enums.ErrorCode;
import com.aipanel.common.exception.BusinessException;
import com.aipanel.user.api.dto.request.RegisterRequest;
import com.aipanel.user.api.dto.request.WebhookRequest;
import com.aipanel.user.api.dto.response.ExternalOrderResponse;
import com.aipanel.user.api.dto.response.UserResponse;
import com.aipanel.user.core.service.AutoDeliveryService;
import com.aipanel.user.core.service.UserService;
import com.aipanel.user.data.mysql.po.DeliveryLogPO;
import com.aipanel.user.data.mysql.po.ExternalOrderPO;
import com.aipanel.user.data.mysql.po.PlatformConfigPO;
import com.aipanel.user.data.mysql.po.ProductSkuPO;
import com.aipanel.user.data.mysql.service.DeliveryLogDataService;
import com.aipanel.user.data.mysql.service.ExternalOrderDataService;
import com.aipanel.user.data.mysql.service.PlatformConfigDataService;
import com.aipanel.user.data.mysql.service.ProductSkuDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoDeliveryServiceImpl implements AutoDeliveryService {

    private final PlatformConfigDataService platformConfigDataService;
    private final ProductSkuDataService productSkuDataService;
    private final ExternalOrderDataService externalOrderDataService;
    private final DeliveryLogDataService deliveryLogDataService;
    private final UserService userService;

    private static final int MAX_RETRY_COUNT = 3;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExternalOrderResponse processWebhook(String platform, WebhookRequest request, String signature, String rawBody) {
        log.info("Processing webhook: platform={}, orderId={}, event={}", platform, request.getOrderId(), request.getEvent());

        // Get platform config
        PlatformConfigPO config = platformConfigDataService.findByPlatform(platform);
        if (config == null || !config.getEnabled()) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "Platform not configured or disabled: " + platform);
        }

        // Verify signature if configured
        if (StringUtils.hasText(config.getWebhookSecret()) && StringUtils.hasText(signature)) {
            if (!verifySignature(platform, signature, rawBody)) {
                throw BusinessException.of(ErrorCode.UNAUTHORIZED, "Invalid webhook signature");
            }
        }

        // Check if order already exists
        ExternalOrderPO existingOrder = externalOrderDataService.findByPlatformAndExternalOrderId(platform, request.getOrderId());
        if (existingOrder != null) {
            log.info("Order already exists: platform={}, orderId={}", platform, request.getOrderId());
            return convertToResponse(existingOrder);
        }

        // Create new order record
        ExternalOrderPO order = new ExternalOrderPO();
        order.setPlatform(platform);
        order.setExternalOrderId(request.getOrderId());
        order.setExternalItemId(request.getItemId());
        order.setSku(request.getSku());
        order.setBuyerNick(request.getBuyerNick());
        order.setBuyerPhone(request.getBuyerPhone());
        order.setBuyerEmail(request.getBuyerEmail());
        order.setBuyerMessage(request.getBuyerMessage());
        order.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
        order.setAmount(request.getAmount());
        order.setStatus(ExternalOrderPO.STATUS_PENDING);
        order.setRetryCount(0);
        order.setRawData(rawBody);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        externalOrderDataService.save(order);
        log.info("Order created: id={}, platform={}, externalOrderId={}", order.getId(), platform, request.getOrderId());

        // Process order if auto-delivery is enabled
        if (config.getAutoDelivery()) {
            return processOrder(order.getId());
        }

        return convertToResponse(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExternalOrderResponse processOrder(Long orderId) {
        ExternalOrderPO order = externalOrderDataService.getById(orderId);
        if (order == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "Order not found");
        }

        if (!ExternalOrderPO.STATUS_PENDING.equals(order.getStatus())) {
            log.info("Order not in pending status: id={}, status={}", orderId, order.getStatus());
            return convertToResponse(order);
        }

        // Update status to processing
        order.setStatus(ExternalOrderPO.STATUS_PROCESSING);
        order.setUpdatedAt(LocalDateTime.now());
        externalOrderDataService.updateById(order);

        try {
            // Get platform config
            PlatformConfigPO config = platformConfigDataService.findByPlatform(order.getPlatform());
            if (config == null) {
                throw new RuntimeException("Platform config not found: " + order.getPlatform());
            }

            // Get product SKU
            ProductSkuPO sku = productSkuDataService.findByPlatformAndSku(order.getPlatform(), order.getSku());
            if (sku == null) {
                throw new RuntimeException("Product SKU not found: " + order.getSku());
            }

            Long userId = order.getUserId();

            // Step 1: Create user if needed
            if (userId == null && config.getAutoCreateUser()) {
                userId = createUserForOrder(order);
                order.setUserId(userId);
                logDeliveryAction(orderId, DeliveryLogPO.ACTION_CREATE_USER, DeliveryLogPO.STATUS_SUCCESS, "User created: " + userId);
            }

            // Step 2: Generate API Key
            String apiKey = generateApiKeyForOrder(order, sku, userId);
            logDeliveryAction(orderId, DeliveryLogPO.ACTION_GENERATE_KEY, DeliveryLogPO.STATUS_SUCCESS, "API Key generated");

            // Step 3: Build delivery content
            String deliveryContent = buildDeliveryContent(config, sku, apiKey);
            order.setDeliveryContent(deliveryContent);

            // Step 4: Send delivery (placeholder - implement actual delivery method)
            sendDelivery(order, config, deliveryContent);
            logDeliveryAction(orderId, DeliveryLogPO.ACTION_SEND_DELIVERY, DeliveryLogPO.STATUS_SUCCESS, "Delivery sent");

            // Mark as delivered
            order.setStatus(ExternalOrderPO.STATUS_DELIVERED);
            order.setDeliveryTime(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            externalOrderDataService.updateById(order);

            log.info("Order delivered successfully: id={}, externalOrderId={}", orderId, order.getExternalOrderId());
            return convertToResponse(order);

        } catch (Exception e) {
            log.error("Failed to process order: id={}, error={}", orderId, e.getMessage(), e);

            // Update order status
            order.setStatus(ExternalOrderPO.STATUS_FAILED);
            order.setFailReason(e.getMessage());
            order.setRetryCount(order.getRetryCount() + 1);
            order.setUpdatedAt(LocalDateTime.now());
            externalOrderDataService.updateById(order);

            logDeliveryAction(orderId, DeliveryLogPO.ACTION_RETRY, DeliveryLogPO.STATUS_FAILED, e.getMessage());

            return convertToResponse(order);
        }
    }

    @Override
    public int retryFailedOrders(int maxRetryCount) {
        List<ExternalOrderPO> failedOrders = externalOrderDataService.findPendingRetry(maxRetryCount);
        int retried = 0;

        for (ExternalOrderPO order : failedOrders) {
            try {
                // Reset to pending for retry
                order.setStatus(ExternalOrderPO.STATUS_PENDING);
                order.setUpdatedAt(LocalDateTime.now());
                externalOrderDataService.updateById(order);

                processOrder(order.getId());
                retried++;
            } catch (Exception e) {
                log.error("Failed to retry order: id={}, error={}", order.getId(), e.getMessage());
            }
        }

        log.info("Retried {} failed orders", retried);
        return retried;
    }

    @Override
    public boolean verifySignature(String platform, String signature, String rawBody) {
        PlatformConfigPO config = platformConfigDataService.findByPlatform(platform);
        if (config == null || !StringUtils.hasText(config.getWebhookSecret())) {
            return true; // No secret configured, skip verification
        }

        try {
            String expectedSignature = computeHmacSha256(rawBody, config.getWebhookSecret());
            return expectedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Failed to verify signature: platform={}, error={}", platform, e.getMessage());
            return false;
        }
    }

    private Long createUserForOrder(ExternalOrderPO order) {
        String username = generateUsername(order);
        String password = generatePassword();

        // Check if user exists by phone or email
        if (StringUtils.hasText(order.getBuyerEmail())) {
            try {
                UserResponse existingUser = userService.getUserByEmail(order.getBuyerEmail());
                return existingUser.getId();
            } catch (BusinessException ignored) {
                // User not found, continue to create
            }
        }

        // Create new user
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(order.getBuyerEmail() != null ? order.getBuyerEmail() : username + "@auto.aipanel.com");
        request.setPassword(password);
        request.setDisplayName(order.getBuyerNick());

        UserResponse user = userService.register(request);
        log.info("User created for order: userId={}, orderId={}", user.getId(), order.getId());

        return user.getId();
    }

    private String generateApiKeyForOrder(ExternalOrderPO order, ProductSkuPO sku, Long userId) {
        // For now, generate a simple API key format
        // In production, this should call the key service
        String keyId = "sk-" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);

        // Store apiKeyId in order (in production, this would be the actual key ID from key service)
        // order.setApiKeyId(apiKeyId);

        log.info("Generated API key for order: orderId={}, userId={}", order.getId(), userId);
        return keyId;
    }

    private String buildDeliveryContent(PlatformConfigPO config, ProductSkuPO sku, String apiKey) {
        String template = config.getDeliveryTemplate();
        if (!StringUtils.hasText(template)) {
            template = getDefaultTemplate();
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(sku.getValidityDays());
        String apiBase = "https://api.aipanel.com/v1";
        String docsUrl = "https://docs.aipanel.com";

        // Support both {placeholder} and ${placeholder} formats
        return template
                // New format: {placeholder}
                .replace("{api_key}", apiKey)
                .replace("{api_base}", apiBase)
                .replace("{product_name}", sku.getProductName() != null ? sku.getProductName() : sku.getSku())
                .replace("{quota_tokens}", sku.getQuotaTokens() != null ? formatNumber(sku.getQuotaTokens()) : "无限制")
                .replace("{quota_requests}", "无限制")
                .replace("{validity_days}", String.valueOf(sku.getValidityDays()))
                .replace("{expire_date}", expiresAt.toLocalDate().toString())
                .replace("{allowed_models}", "全部模型")
                .replace("{docs_url}", docsUrl)
                // Legacy format: ${placeholder}
                .replace("${apiKey}", apiKey)
                .replace("${apiBase}", apiBase)
                .replace("${productName}", sku.getProductName() != null ? sku.getProductName() : sku.getSku())
                .replace("${quotaTokens}", sku.getQuotaTokens() != null ? formatNumber(sku.getQuotaTokens()) : "无限制")
                .replace("${quotaRequests}", "无限制")
                .replace("${validityDays}", String.valueOf(sku.getValidityDays()))
                .replace("${expiresAt}", expiresAt.toLocalDate().toString())
                .replace("${allowedModels}", "全部模型")
                .replace("${docsUrl}", docsUrl);
    }

    private void sendDelivery(ExternalOrderPO order, PlatformConfigPO config, String deliveryContent) {
        // Determine delivery method
        String deliveryMethod = "platform_message"; // default

        if (StringUtils.hasText(order.getBuyerEmail())) {
            deliveryMethod = "email";
            // TODO: Implement email sending
            log.info("Would send email to: {}", order.getBuyerEmail());
        } else if (StringUtils.hasText(order.getBuyerPhone())) {
            deliveryMethod = "sms";
            // TODO: Implement SMS sending
            log.info("Would send SMS to: {}", order.getBuyerPhone());
        }

        order.setDeliveryMethod(deliveryMethod);
        log.info("Delivery method: {}, orderId={}", deliveryMethod, order.getId());
    }

    private void logDeliveryAction(Long orderId, String action, String status, String message) {
        DeliveryLogPO log = new DeliveryLogPO();
        log.setOrderId(orderId);
        log.setAction(action);
        log.setStatus(status);
        log.setMessage(message);
        log.setCreatedAt(LocalDateTime.now());
        deliveryLogDataService.save(log);
    }

    private String generateUsername(ExternalOrderPO order) {
        String base = order.getBuyerNick();
        if (!StringUtils.hasText(base)) {
            base = "user";
        }
        // Clean up and add unique suffix
        base = base.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
        if (base.length() < 3) {
            base = "user";
        }
        return base + "_" + System.currentTimeMillis() % 100000;
    }

    private String generatePassword() {
        return "Ap" + UUID.randomUUID().toString().replace("-", "").substring(0, 10) + "1!";
    }

    private String getDefaultTemplate() {
        return """
            【AI Panel 订单发货通知】

            感谢您的购买！您的 API 密钥信息如下：

            API Key: ${apiKey}
            产品名称: ${productName}
            Token 额度: ${quotaTokens}
            有效期至: ${expiresAt}
            支持模型: ${allowedModels}

            使用方法：
            1. 在请求头中添加: Authorization: Bearer ${apiKey}
            2. API 地址: https://api.aipanel.com/v1/chat/completions

            如有问题，请联系客服。
            """;
    }

    private String computeHmacSha256(String data, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }

    private String formatNumber(Long number) {
        if (number >= 1000000) {
            return (number / 1000000) + "M";
        } else if (number >= 1000) {
            return (number / 1000) + "K";
        }
        return String.valueOf(number);
    }

    private ExternalOrderResponse convertToResponse(ExternalOrderPO order) {
        return ExternalOrderResponse.builder()
                .id(order.getId())
                .platform(order.getPlatform())
                .externalOrderId(order.getExternalOrderId())
                .externalItemId(order.getExternalItemId())
                .sku(order.getSku())
                .buyerNick(order.getBuyerNick())
                .buyerPhone(order.getBuyerPhone())
                .buyerEmail(order.getBuyerEmail())
                .buyerMessage(order.getBuyerMessage())
                .quantity(order.getQuantity())
                .amount(order.getAmount())
                .userId(order.getUserId())
                .apiKeyId(order.getApiKeyId())
                .status(order.getStatus())
                .deliveryMethod(order.getDeliveryMethod())
                .deliveryContent(order.getDeliveryContent())
                .deliveryTime(order.getDeliveryTime())
                .failReason(order.getFailReason())
                .retryCount(order.getRetryCount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
