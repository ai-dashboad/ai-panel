package com.aipanel.channel.api.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Provider Configuration Response DTO
 */
@Data
public class ProviderConfigResponse {

    private Long id;
    private String name;
    private String type;
    private String baseUrl;
    private Boolean enabled;
    private Integer priority;
    private Integer timeoutSeconds;
    private Integer retryCount;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Number of API keys configured
     */
    private Integer keyCount;

    /**
     * Number of active (available) keys
     */
    private Integer activeKeyCount;

    /**
     * List of configured models
     */
    private List<String> models;
}
