package com.aipanel.user.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Internal API Key Creation Request")
public class InternalCreateKeyRequest {

    @NotNull(message = "User ID is required")
    @Schema(description = "Target user ID")
    private Long userId;

    @Schema(description = "API Key name", example = "Auto-generated Key")
    private String name;

    @Schema(description = "Key description")
    private String description;

    @Schema(description = "Token quota (null for unlimited)")
    private Long quotaTokens;

    @Schema(description = "Request quota (null for unlimited)")
    private Long quotaRequests;

    @Schema(description = "Allowed models (comma-separated, null for all)")
    private String allowedModels;

    @Schema(description = "Rate limit per minute")
    private Integer rateLimitPerMinute;

    @Schema(description = "Expiration time")
    private LocalDateTime expiresAt;

    @Schema(description = "External order ID for tracking")
    private String externalOrderId;
}
