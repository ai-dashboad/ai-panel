package com.aipanel.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login Response")
public class LoginResponse {

    @Schema(description = "Access token (JWT)", example = "eyJhbGciOiJIUzI1NiIs...")
    private String accessToken;

    @Schema(description = "Refresh token", example = "rt_xxxxxxxxxxxx")
    private String refreshToken;

    @Schema(description = "Token type", example = "Bearer")
    private String tokenType;

    @Schema(description = "Access token expiration time in seconds", example = "3600")
    private Long expiresIn;

    @Schema(description = "User information")
    private UserResponse user;

    /**
     * Create login response with bearer token
     */
    public static LoginResponse of(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(expiresIn)
            .user(user)
            .build();
    }
}
