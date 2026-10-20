package com.aipanel.user.web.controller;

import com.aipanel.common.constant.CommonConstants;
import com.aipanel.common.domain.R;
import com.aipanel.user.api.dto.request.UpdateUserRequest;
import com.aipanel.user.api.dto.response.UserResponse;
import com.aipanel.user.core.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * User management controller
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User profile management endpoints")
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public R<UserResponse> getCurrentUser(
            @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) Long userId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        // Try to get userId from header first, then from JWT token
        if (userId == null && authorization != null && authorization.startsWith("Bearer ")) {
            userId = userService.getUserIdFromToken(authorization.substring(7));
        }
        if (userId == null) {
            return R.fail(401, "Not authenticated");
        }
        UserResponse user = userService.getUserById(userId);
        return R.ok(user);
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public R<UserResponse> updateCurrentUser(
            @RequestBody @Valid UpdateUserRequest request,
            @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) Long userId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        // Try to get userId from header first, then from JWT token
        if (userId == null && authorization != null && authorization.startsWith("Bearer ")) {
            userId = userService.getUserIdFromToken(authorization.substring(7));
        }
        if (userId == null) {
            return R.fail(401, "Not authenticated");
        }
        UserResponse user = userService.updateUser(userId, request);
        return R.ok(user);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public R<UserResponse> getUserById(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long id,
            @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) Long currentUserId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (currentUserId == null && authorization != null && authorization.startsWith("Bearer ")) {
            currentUserId = userService.getUserIdFromToken(authorization.substring(7));
        }
        if (currentUserId == null) {
            return R.fail(401, "Not authenticated");
        }
        // Profiles (email, plan, ...) are visible only to their owner or an admin.
        if (!currentUserId.equals(id) && !userService.isAdmin(currentUserId)) {
            return R.fail(403, "Not authorized to view this user");
        }
        UserResponse user = userService.getUserById(id);
        return R.ok(user);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user profile")
    public R<UserResponse> updateUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long id,
            @RequestBody @Valid UpdateUserRequest request,
            @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) Long currentUserId) {
        // Check if user is updating their own profile
        if (currentUserId == null || !currentUserId.equals(id)) {
            return R.fail(403, "Not authorized to update this user");
        }
        UserResponse user = userService.updateUser(id, request);
        return R.ok(user);
    }

    @GetMapping("/internal/{id}")
    @Operation(summary = "Get user for internal service call")
    public R<UserResponse> getInternalUser(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return R.ok(user);
    }
}
