package com.aipanel.gateway.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * API Key validation result DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "API Key Validation Result")
public class ApiKeyValidationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Validation success", example = "true")
    private boolean valid;

    @Schema(description = "User ID associated with the key", example = "12345")
    private Long userId;

    @Schema(description = "API Key ID", example = "key_abc123")
    private String keyId;

    @Schema(description = "User's plan type", example = "PRO")
    private String planType;

    @Schema(description = "Allowed models for this key")
    private List<String> allowedModels;

    @Schema(description = "Custom rate limit per minute (null means use plan default)", example = "60")
    private Integer rateLimitPerMinute;

    @Schema(description = "Error code if invalid")
    private Integer errorCode;

    @Schema(description = "Error message if invalid")
    private String errorMessage;

    /**
     * Create a valid result
     */
    public static ApiKeyValidationResult valid(Long userId, String keyId, String planType,
                                                List<String> allowedModels, Integer rateLimitPerMinute) {
        return ApiKeyValidationResult.builder()
            .valid(true)
            .userId(userId)
            .keyId(keyId)
            .planType(planType)
            .allowedModels(allowedModels)
            .rateLimitPerMinute(rateLimitPerMinute)
            .build();
    }

    /**
     * Create an invalid result
     */
    public static ApiKeyValidationResult invalid(int errorCode, String errorMessage) {
        return ApiKeyValidationResult.builder()
            .valid(false)
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .build();
    }

    /**
     * Check if a specific model is allowed
     */
    public boolean isModelAllowed(String model) {
        if (!valid) {
            return false;
        }
        if (allowedModels == null || allowedModels.isEmpty()) {
            return true; // Empty list means all models allowed
        }
        return allowedModels.contains(model);
    }
}
