package com.aipanel.user.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * User login request DTO
 */
@Data
@Schema(description = "User Login Request")
public class LoginRequest {

    @NotBlank(message = "Username or email is required")
    @Size(max = 100, message = "Username/email must not exceed 100 characters")
    @Schema(description = "Username or email", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    @Size(max = 100, message = "Password must not exceed 100 characters")
    @Schema(description = "Password", example = "MyPassword123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
