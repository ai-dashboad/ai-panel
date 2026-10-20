package com.aipanel.user.core.service;

/**
 * JWT token service interface
 */
public interface JwtService {

    /**
     * Generate access token
     *
     * @param userId User ID
     * @param username Username
     * @return Access token
     */
    String generateAccessToken(Long userId, String username, String role);

    /**
     * Generate refresh token
     *
     * @param userId User ID
     * @return Refresh token
     */
    String generateRefreshToken(Long userId);

    /**
     * Validate access token and extract user ID
     *
     * @param token Access token
     * @return User ID if valid, null otherwise
     */
    Long validateAccessToken(String token);

    /**
     * Validate refresh token and extract user ID
     *
     * @param token Refresh token
     * @return User ID if valid, null otherwise
     */
    Long validateRefreshToken(String token);

    /**
     * Extract username from token
     *
     * @param token Access token
     * @return Username
     */
    String extractUsername(String token);

    /**
     * Get access token expiration time in seconds
     *
     * @return Expiration time
     */
    long getAccessTokenExpiration();

    /**
     * Check if token is expired
     *
     * @param token Token to check
     * @return true if expired
     */
    boolean isTokenExpired(String token);
}
