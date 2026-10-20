package com.aipanel.user.web.controller;

import com.aipanel.common.domain.R;
import com.aipanel.user.api.dto.request.LoginRequest;
import com.aipanel.user.api.dto.request.RefreshTokenRequest;
import com.aipanel.user.api.dto.request.RegisterRequest;
import com.aipanel.user.api.dto.response.LoginResponse;
import com.aipanel.user.api.dto.response.UserResponse;
import com.aipanel.user.core.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication endpoints")
@Slf4j
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register new user")
    public R<UserResponse> register(@RequestBody @Valid RegisterRequest request) {
        log.info("User registration: username={}", request.getUsername());
        UserResponse user = userService.register(request);
        return R.ok(user);
    }

    @PostMapping("/login")
    @Operation(summary = "User login")
    public R<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        log.info("User login: usernameOrEmail={}", request.getUsernameOrEmail());
        LoginResponse response = userService.login(request);
        return R.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public R<LoginResponse> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        LoginResponse response = userService.refreshToken(request.getRefreshToken());
        return R.ok(response);
    }
}
