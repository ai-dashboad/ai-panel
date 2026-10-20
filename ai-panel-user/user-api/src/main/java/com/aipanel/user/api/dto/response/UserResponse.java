package com.aipanel.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User Information Response")
public class UserResponse {

    @Schema(description = "User ID", example = "12345")
    private Long id;

    @Schema(description = "Username", example = "john_doe")
    private String username;

    @Schema(description = "Email address", example = "john@example.com")
    private String email;

    @Schema(description = "Display name", example = "John Doe")
    private String displayName;

    @Schema(description = "User status: 1-active, 0-inactive", example = "1")
    private Integer status;

    @Schema(description = "Current plan type", example = "PRO")
    private String planType;

    @Schema(description = "Plan expiration date")
    private LocalDateTime planExpiresAt;

    @Schema(description = "User timezone", example = "Asia/Shanghai")
    private String timezone;

    @Schema(description = "Preferred language", example = "en")
    private String language;

    @Schema(description = "Account creation time")
    private LocalDateTime createdAt;

    @Schema(description = "Last login time")
    private LocalDateTime lastLoginAt;
}
