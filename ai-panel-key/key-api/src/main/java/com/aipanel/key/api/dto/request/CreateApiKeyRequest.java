package com.aipanel.key.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Create API Key request DTO
 */
@Data
@Schema(description = "Create API Key Request")
public class CreateApiKeyRequest {

    @NotBlank(message = "Key name is required")
    @Size(min = 1, max = 100, message = "Name must be 1-100 characters")
    @Schema(description = "API Key name", example = "Production Key", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 20, message = "Maximum 20 models allowed")
    @Schema(description = "Allowed models (empty for all)", example = "[\"gpt-4\", \"claude-3-opus\"]")
    private List<@Pattern(regexp = "^[a-zA-Z0-9._-]+$") String> allowedModels;

    @Min(value = 1, message = "Rate limit must be at least 1")
    @Max(value = 10000, message = "Rate limit cannot exceed 10000")
    @Schema(description = "Custom rate limit per minute (null for plan default)", example = "60")
    private Integer rateLimitPerMinute;

    @Future(message = "Expiration date must be in the future")
    @Schema(description = "Expiration time (null for no expiration)")
    private LocalDateTime expiresAt;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Key description", example = "API Key for production environment")
    private String description;
}
