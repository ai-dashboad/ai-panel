package com.aipanel.key.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Validate API Key request DTO (Internal)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Validate API Key Request")
public class ValidateApiKeyRequest {

    @NotBlank(message = "API Key is required")
    @Schema(description = "Full API Key", example = "sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", requiredMode = Schema.RequiredMode.REQUIRED)
    private String apiKey;

    @Schema(description = "Model to access (for permission check)", example = "gpt-4")
    private String model;
}
