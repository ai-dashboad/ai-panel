package com.aipanel.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Platform Configuration Response")
public class PlatformConfigResponse {

    @Schema(description = "Configuration ID", example = "1")
    private Long id;

    @Schema(description = "Platform identifier", example = "taobao")
    private String platform;

    @Schema(description = "Configuration name", example = "淘宝店铺1")
    private String name;

    @Schema(description = "Platform app key (masked)", example = "app_***")
    private String appKey;

    @Schema(description = "Webhook URL for this platform")
    private String webhookUrl;

    @Schema(description = "Delivery message template")
    private String deliveryTemplate;

    @Schema(description = "Auto create user account for new buyers")
    private Boolean autoCreateUser;

    @Schema(description = "Auto deliver after order paid")
    private Boolean autoDelivery;

    @Schema(description = "Configuration enabled status")
    private Boolean enabled;

    @Schema(description = "Additional platform-specific config")
    private String config;

    @Schema(description = "Creation time")
    private LocalDateTime createdAt;

    @Schema(description = "Last update time")
    private LocalDateTime updatedAt;
}
