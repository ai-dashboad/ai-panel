package com.aipanel.channel.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Provider Configuration Request DTO
 */
@Data
public class ProviderConfigRequest {

    /**
     * Provider name for display
     */
    @NotBlank(message = "Provider name is required")
    private String name;

    /**
     * Provider type: openai, anthropic, deepseek, moonshot, etc.
     */
    @NotBlank(message = "Provider type is required")
    private String type;

    /**
     * Base URL for API calls
     */
    private String baseUrl;

    /**
     * Whether this provider is enabled
     */
    private Boolean enabled = true;

    /**
     * Priority for fallback routing (lower = higher priority)
     */
    private Integer priority = 100;

    /**
     * Request timeout in seconds
     */
    private Integer timeoutSeconds = 60;

    /**
     * Number of retries on failure
     */
    private Integer retryCount = 3;

    /**
     * Description of this provider
     */
    private String description;
}
