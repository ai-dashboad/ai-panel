package com.aipanel.user.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Update user request DTO
 */
@Data
@Schema(description = "Update User Request")
public class UpdateUserRequest {

    @Size(max = 100, message = "Display name must not exceed 100 characters")
    @Schema(description = "Display name", example = "John Smith")
    private String displayName;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Schema(description = "New email address", example = "john.new@example.com")
    private String email;

    @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
             message = "Password must contain at least one lowercase letter, one uppercase letter, and one digit")
    @Schema(description = "New password (leave empty to keep current)")
    private String newPassword;

    @Size(max = 100, message = "Current password must not exceed 100 characters")
    @Schema(description = "Current password (required when changing password)")
    private String currentPassword;

    @Size(max = 20, message = "Timezone must not exceed 20 characters")
    @Schema(description = "User's timezone", example = "Asia/Shanghai")
    private String timezone;

    @Size(max = 10, message = "Language code must not exceed 10 characters")
    @Schema(description = "Preferred language", example = "en")
    private String language;
}
