package com.aipanel.key.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Update API Key request DTO
 */
@Data
@Schema(description = "Update API Key Request")
public class UpdateApiKeyRequest {

    @Size(min = 1, max = 100, message = "Name must be 1-100 characters")
    @Schema(description = "API Key name", example = "Updated Key Name")
    private String name;

    @Size(max = 20, message = "Maximum 20 models allowed")
    @Schema(description = "Allowed models (empty for all)")
    private List<@Pattern(regexp = "^[a-zA-Z0-9._-]+$") String> allowedModels;

    @Min(value = 1, message = "Rate limit must be at least 1")
    @Max(value = 10000, message = "Rate limit cannot exceed 10000")
    @Schema(description = "Custom rate limit per minute")
    private Integer rateLimitPerMinute;

    @Schema(description = "New expiration time (null for no expiration)")
    private LocalDateTime expiresAt;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Key description")
    private String description;

    @Schema(description = "Key status: 1-active, 0-disabled")
    private Integer status;
}
