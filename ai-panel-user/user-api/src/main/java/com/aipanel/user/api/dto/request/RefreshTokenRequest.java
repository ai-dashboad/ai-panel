package com.aipanel.user.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Refresh token request DTO
 */
@Data
@Schema(description = "Refresh Token Request")
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    @Schema(description = "Refresh token", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}
