package com.aipanel.user.core.service.impl;

import com.aipanel.user.core.security.UserRole;
import com.aipanel.common.enums.ErrorCode;
import com.aipanel.common.enums.PlanType;
import com.aipanel.common.exception.BusinessException;
import com.aipanel.user.api.dto.request.LoginRequest;
import com.aipanel.user.api.dto.request.RegisterRequest;
import com.aipanel.user.api.dto.request.UpdateUserRequest;
import com.aipanel.user.api.dto.response.LoginResponse;
import com.aipanel.user.api.dto.response.UserResponse;
import com.aipanel.user.core.service.JwtService;
import com.aipanel.user.core.service.UserService;
import com.aipanel.user.data.mysql.po.UserPO;
import com.aipanel.user.data.mysql.po.UserPlanPO;
import com.aipanel.user.data.mysql.service.UserDataService;
import com.aipanel.user.data.mysql.service.UserPlanDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * User service implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserDataService userDataService;
    private final UserPlanDataService userPlanDataService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResponse register(RegisterRequest request) {
        // Check if username exists
        if (userDataService.existsByUsername(request.getUsername())) {
            throw BusinessException.of(ErrorCode.USERNAME_EXISTS, "Username already taken");
        }

        // Check if email exists
        if (userDataService.existsByEmail(request.getEmail())) {
            throw BusinessException.of(ErrorCode.EMAIL_EXISTS, "Email already registered");
        }

        // Create user
        UserPO user = new UserPO();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName() != null ? request.getDisplayName() : request.getUsername());
        user.setStatus(1);
        user.setRole(UserRole.USER);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userDataService.save(user);
        log.info("User registered: username={}, id={}", user.getUsername(), user.getId());

        // Create default free plan
        UserPlanPO plan = new UserPlanPO();
        plan.setUserId(user.getId());
        plan.setPlanType(PlanType.FREE.getCode());
        plan.setStartDate(LocalDate.now());
        plan.setIsActive(1);
        userPlanDataService.save(plan);

        return convertToResponse(user, plan);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // Find user by username or email
        UserPO user = userDataService.findByUsernameOrEmail(request.getUsernameOrEmail());
        if (user == null) {
            throw BusinessException.of(ErrorCode.USER_NOT_FOUND, "Invalid username or email");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw BusinessException.of(ErrorCode.INVALID_PASSWORD, "Invalid password");
        }

        // Check user status
        if (user.getStatus() != 1) {
            throw BusinessException.of(ErrorCode.USER_DISABLED, "User account is disabled");
        }

        // Get user plan
        UserPlanPO plan = userPlanDataService.findActiveByUserId(user.getId());

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // Update last login
        updateLastLogin(user.getId());

        UserResponse userResponse = convertToResponse(user, plan);
        log.info("User logged in: username={}", user.getUsername());

        return LoginResponse.of(accessToken, refreshToken, jwtService.getAccessTokenExpiration(), userResponse);
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        // Validate refresh token
        Long userId = jwtService.validateRefreshToken(refreshToken);
        if (userId == null) {
            throw BusinessException.of(ErrorCode.REFRESH_TOKEN_INVALID, "Invalid refresh token");
        }

        // Get user
        UserPO user = userDataService.getById(userId);
        if (user == null) {
            throw BusinessException.of(ErrorCode.USER_NOT_FOUND, "User not found");
        }

        if (user.getStatus() != 1) {
            throw BusinessException.of(ErrorCode.USER_DISABLED, "User account is disabled");
        }

        // Get user plan
        UserPlanPO plan = userPlanDataService.findActiveByUserId(user.getId());

        // Generate new tokens
        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId());

        UserResponse userResponse = convertToResponse(user, plan);

        return LoginResponse.of(newAccessToken, newRefreshToken, jwtService.getAccessTokenExpiration(), userResponse);
    }

    @Override
    public UserResponse getUserById(Long userId) {
        UserPO user = userDataService.getById(userId);
        if (user == null) {
            throw BusinessException.of(ErrorCode.USER_NOT_FOUND, "User not found");
        }

        UserPlanPO plan = userPlanDataService.findActiveByUserId(userId);
        return convertToResponse(user, plan);
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        UserPO user = userDataService.findByUsername(username);
        if (user == null) {
            throw BusinessException.of(ErrorCode.USER_NOT_FOUND, "User not found");
        }

        UserPlanPO plan = userPlanDataService.findActiveByUserId(user.getId());
        return convertToResponse(user, plan);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        UserPO user = userDataService.findByEmail(email);
        if (user == null) {
            throw BusinessException.of(ErrorCode.USER_NOT_FOUND, "User not found");
        }

        UserPlanPO plan = userPlanDataService.findActiveByUserId(user.getId());
        return convertToResponse(user, plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        UserPO user = userDataService.getById(userId);
        if (user == null) {
            throw BusinessException.of(ErrorCode.USER_NOT_FOUND, "User not found");
        }

        // Update fields
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userDataService.existsByEmail(request.getEmail())) {
                throw BusinessException.of(ErrorCode.EMAIL_EXISTS, "Email already registered");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getNewPassword() != null) {
            if (request.getCurrentPassword() == null) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "Current password is required");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw BusinessException.of(ErrorCode.INVALID_PASSWORD, "Current password is incorrect");
            }
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }

        if (request.getTimezone() != null) {
            user.setTimezone(request.getTimezone());
        }

        if (request.getLanguage() != null) {
            user.setLanguage(request.getLanguage());
        }

        user.setUpdatedAt(LocalDateTime.now());
        userDataService.updateById(user);

        UserPlanPO plan = userPlanDataService.findActiveByUserId(userId);
        return convertToResponse(user, plan);
    }

    @Override
    public boolean usernameExists(String username) {
        return userDataService.existsByUsername(username);
    }

    @Override
    public boolean emailExists(String email) {
        return userDataService.existsByEmail(email);
    }

    @Override
    public void updateLastLogin(Long userId) {
        userDataService.updateLastLogin(userId, LocalDateTime.now());
    }

    @Override
    public Long getUserIdFromToken(String token) {
        try {
            return jwtService.validateAccessToken(token);
        } catch (Exception e) {
            log.debug("Invalid access token: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        UserPO user = userDataService.getById(userId);
        return user != null
            && Integer.valueOf(1).equals(user.getStatus())
            && UserRole.isAdmin(user.getRole());
    }

    private UserResponse convertToResponse(UserPO user, UserPlanPO plan) {
        UserResponse.UserResponseBuilder builder = UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .displayName(user.getDisplayName())
            .role(UserRole.normalize(user.getRole()))
            .status(user.getStatus())
            .timezone(user.getTimezone())
            .language(user.getLanguage())
            .createdAt(user.getCreatedAt())
            .lastLoginAt(user.getLastLoginAt());

        if (plan != null) {
            builder.planType(plan.getPlanType());
            if (plan.getEndDate() != null) {
                builder.planExpiresAt(plan.getEndDate().atStartOfDay());
            }
        } else {
            builder.planType(PlanType.FREE.getCode());
        }

        return builder.build();
    }
}
