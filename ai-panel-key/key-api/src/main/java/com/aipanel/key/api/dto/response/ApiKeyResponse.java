package com.aipanel.key.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API Key response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "API Key Response")
public class ApiKeyResponse {

    @Schema(description = "Key ID", example = "key_abc12345")
    private String id;

    @Schema(description = "Key prefix for display", example = "sk-...abc1")
    private String keyPrefix;

    @Schema(description = "Full key (only returned on creation)", example = "sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
    private String fullKey;

    @Schema(description = "Key name", example = "Production Key")
    private String name;

    @Schema(description = "Key description")
    private String description;

    @Schema(description = "Status: 1-active, 0-disabled", example = "1")
    private Integer status;

    @Schema(description = "Allowed models (empty for all)")
    private List<String> allowedModels;

    @Schema(description = "Rate limit per minute", example = "60")
    private Integer rateLimitPerMinute;

    @Schema(description = "Expiration time")
    private LocalDateTime expiresAt;

    @Schema(description = "Last used time")
    private LocalDateTime lastUsedAt;

    @Schema(description = "Creation time")
    private LocalDateTime createdAt;
}
