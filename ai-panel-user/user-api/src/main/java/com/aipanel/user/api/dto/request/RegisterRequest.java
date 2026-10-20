package com.aipanel.user.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * User registration request DTO
 */
@Data
@Schema(description = "User Registration Request")
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    @Schema(description = "Username", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Schema(description = "Email address", example = "john@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
             message = "Password must contain at least one lowercase letter, one uppercase letter, and one digit")
    @Schema(description = "Password (min 8 chars, must contain upper, lower, and digit)",
            example = "MyPassword123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Size(max = 100, message = "Display name must not exceed 100 characters")
    @Schema(description = "Display name", example = "John Doe")
    private String displayName;
}
