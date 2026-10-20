package com.aipanel.key.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * API Key validation response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "API Key Validation Response")
public class ApiKeyValidationResponse {

    @Schema(description = "Is the key valid", example = "true")
    private boolean valid;

    @Schema(description = "User ID", example = "12345")
    private Long userId;

    @Schema(description = "Key ID", example = "key_abc12345")
    private String keyId;

    @Schema(description = "User's plan type", example = "PRO")
    private String planType;

    @Schema(description = "Allowed models for this key")
    private List<String> allowedModels;

    @Schema(description = "Rate limit per minute", example = "60")
    private Integer rateLimitPerMinute;

    @Schema(description = "Is model allowed (when model is specified)", example = "true")
    private Boolean modelAllowed;

    @Schema(description = "Error code if invalid")
    private Integer errorCode;

    @Schema(description = "Error message if invalid")
    private String errorMessage;

    public static ApiKeyValidationResponse valid(Long userId, String keyId, String planType,
                                                  List<String> allowedModels, Integer rateLimitPerMinute) {
        return ApiKeyValidationResponse.builder()
            .valid(true)
            .userId(userId)
            .keyId(keyId)
            .planType(planType)
            .allowedModels(allowedModels)
            .rateLimitPerMinute(rateLimitPerMinute)
            .build();
    }

    public static ApiKeyValidationResponse invalid(int errorCode, String errorMessage) {
        return ApiKeyValidationResponse.builder()
            .valid(false)
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .build();
    }

    public ApiKeyValidationResponse withModelCheck(boolean modelAllowed) {
        this.modelAllowed = modelAllowed;
        return this;
    }
}
