package com.aipanel.user.core.service;

import com.aipanel.user.api.dto.request.LoginRequest;
import com.aipanel.user.api.dto.request.RegisterRequest;
import com.aipanel.user.api.dto.request.UpdateUserRequest;
import com.aipanel.user.api.dto.response.LoginResponse;
import com.aipanel.user.api.dto.response.UserResponse;

/**
 * User service interface
 */
public interface UserService {

    /**
     * Register a new user
     *
     * @param request Registration request
     * @return Created user response
     */
    UserResponse register(RegisterRequest request);

    /**
     * User login
     *
     * @param request Login request
     * @return Login response with tokens
     */
    LoginResponse login(LoginRequest request);

    /**
     * Refresh access token
     *
     * @param refreshToken Refresh token
     * @return New login response with tokens
     */
    LoginResponse refreshToken(String refreshToken);

    /**
     * Get user by ID
     *
     * @param userId User ID
     * @return User response
     */
    UserResponse getUserById(Long userId);

    /**
     * Get user by username
     *
     * @param username Username
     * @return User response
     */
    UserResponse getUserByUsername(String username);

    /**
     * Get user by email
     *
     * @param email Email
     * @return User response
     */
    UserResponse getUserByEmail(String email);

    /**
     * Update user
     *
     * @param userId User ID
     * @param request Update request
     * @return Updated user response
     */
    UserResponse updateUser(Long userId, UpdateUserRequest request);

    /**
     * Check if username exists
     *
     * @param username Username
     * @return true if exists
     */
    boolean usernameExists(String username);

    /**
     * Check if email exists
     *
     * @param email Email
     * @return true if exists
     */
    boolean emailExists(String email);

    /**
     * Update last login time
     *
     * @param userId User ID
     */
    void updateLastLogin(Long userId);

    /**
     * Extract user ID from JWT token
     *
     * @param token JWT access token
     * @return User ID or null if invalid
     */
    Long getUserIdFromToken(String token);
}
