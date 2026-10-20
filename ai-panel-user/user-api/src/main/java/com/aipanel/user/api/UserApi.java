package com.aipanel.user.api;

import com.aipanel.common.domain.R;
import com.aipanel.user.api.dto.request.LoginRequest;
import com.aipanel.user.api.dto.request.RegisterRequest;
import com.aipanel.user.api.dto.request.UpdateUserRequest;
import com.aipanel.user.api.dto.response.LoginResponse;
import com.aipanel.user.api.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * User Service Feign Client
 */
@Tag(name = "User Management", description = "User registration, login, and profile management")
@FeignClient(name = "ai-panel-user")
public interface UserApi {

    @PostMapping("/api/v1/auth/register")
    @Operation(summary = "Register new user", description = "Create a new user account")
    R<UserResponse> register(@RequestBody @Valid RegisterRequest request);

    @PostMapping("/api/v1/auth/login")
    @Operation(summary = "User login", description = "Authenticate user and get access token")
    R<LoginResponse> login(@RequestBody @Valid LoginRequest request);

    @PostMapping("/api/v1/auth/refresh")
    @Operation(summary = "Refresh token", description = "Refresh access token using refresh token")
    R<LoginResponse> refreshToken(@RequestHeader("X-Refresh-Token") String refreshToken);

    @GetMapping("/api/v1/users/me")
    @Operation(summary = "Get current user", description = "Get current authenticated user's profile")
    R<UserResponse> getCurrentUser();

    @GetMapping("/api/v1/users/{id}")
    @Operation(summary = "Get user by ID", description = "Get user profile by user ID")
    R<UserResponse> getUserById(
        @Parameter(description = "User ID", required = true)
        @PathVariable Long id);

    @PutMapping("/api/v1/users/{id}")
    @Operation(summary = "Update user", description = "Update user profile")
    R<UserResponse> updateUser(
        @Parameter(description = "User ID", required = true)
        @PathVariable Long id,
        @RequestBody @Valid UpdateUserRequest request);

    @GetMapping("/api/v1/users/internal/{id}")
    @Operation(summary = "Get user for internal service", description = "Internal API for service-to-service calls")
    R<UserResponse> getInternalUser(@PathVariable Long id);
}
