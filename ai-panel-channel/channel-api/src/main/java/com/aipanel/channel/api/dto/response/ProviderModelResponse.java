package com.aipanel.channel.api.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A model registered on a provider.
 */
@Data
public class ProviderModelResponse {
    private Long id;
    private Long providerId;
    private String modelId;
    private String modelName;
    private Boolean enabled;
    private BigDecimal inputPricePerMillion;
    private BigDecimal outputPricePerMillion;
    private Integer maxTokens;
    private Integer contextWindow;
    private Boolean supportsVision;
    private Boolean supportsFunctionCall;
    private LocalDateTime createdAt;
}
