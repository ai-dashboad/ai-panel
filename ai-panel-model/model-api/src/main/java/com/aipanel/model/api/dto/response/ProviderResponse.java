package com.aipanel.model.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Provider Response
 */
@Data
@Builder
@Schema(description = "Provider Response")
public class ProviderResponse {

    @Schema(description = "Provider code")
    private String providerCode;

    @Schema(description = "Provider name")
    private String providerName;

    @Schema(description = "API base URL")
    private String baseUrl;

    @Schema(description = "Priority")
    private Integer priority;

    @Schema(description = "Rate limit per minute")
    private Integer rateLimitPerMinute;

    @Schema(description = "Timeout in milliseconds")
    private Integer timeout;

    @Schema(description = "Whether provider is enabled")
    private Boolean enabled;

    @Schema(description = "Health status")
    private String healthStatus;

    @Schema(description = "Last health check time")
    private LocalDateTime lastHealthCheck;

    @Schema(description = "Model count")
    private Integer modelCount;
}
