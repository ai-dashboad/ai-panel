package com.aipanel.user.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Platform Configuration Request")
public class PlatformConfigRequest {

    @NotBlank(message = "Platform is required")
    @Size(max = 30, message = "Platform must not exceed 30 characters")
    @Schema(description = "Platform identifier (taobao, xianyu, etc.)", example = "taobao", requiredMode = Schema.RequiredMode.REQUIRED)
    private String platform;

    @NotBlank(message = "Name is required")
    @Size(max = 50, message = "Name must not exceed 50 characters")
    @Schema(description = "Configuration name", example = "淘宝店铺1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 100, message = "App key must not exceed 100 characters")
    @Schema(description = "Platform app key", example = "app_key_xxx")
    private String appKey;

    @Size(max = 200, message = "App secret must not exceed 200 characters")
    @Schema(description = "Platform app secret", example = "app_secret_xxx")
    private String appSecret;

    @Size(max = 100, message = "Webhook secret must not exceed 100 characters")
    @Schema(description = "Webhook signature secret for verification", example = "webhook_secret_xxx")
    private String webhookSecret;

    @Schema(description = "Delivery message template with placeholders", example = "您的API Key: ${apiKey}\n有效期至: ${expiresAt}")
    private String deliveryTemplate;

    @Schema(description = "Auto create user account for new buyers", example = "true")
    private Boolean autoCreateUser = true;

    @Schema(description = "Auto deliver after order paid", example = "true")
    private Boolean autoDelivery = true;

    @Schema(description = "Enable this configuration", example = "true")
    private Boolean enabled = true;

    @Schema(description = "Additional platform-specific config in JSON format")
    private String config;
}
