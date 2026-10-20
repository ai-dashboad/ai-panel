package com.aipanel.user.core.service.impl;

import com.aipanel.user.core.service.JwtService;
import com.aipanel.user.core.security.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT service implementation
 */
@Service
@Slf4j
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration:3600}")
    private long accessTokenExpiration; // seconds

    @Value("${jwt.refresh-token-expiration:604800}")
    private long refreshTokenExpiration; // seconds (7 days)

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public String generateAccessToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration * 1000);

        return Jwts.builder()
            .subject(userId.toString())
            .claim("username", username)
            // Role claim lets the gateway (and workers) enforce admin-only routes without a DB lookup.
            .claim("role", UserRole.normalize(role))
            .claim("type", "access")
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
    }

    @Override
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration * 1000);

        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", "refresh")
            .id(UUID.randomUUID().toString())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
    }

    @Override
    public Long validateAccessToken(String token) {
        try {
            Claims claims = parseToken(token);
            String type = claims.get("type", String.class);
            if (!"access".equals(type)) {
                return null;
            }
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            log.debug("Access token validation failed: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Long validateRefreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            String type = claims.get("type", String.class);
            if (!"refresh".equals(type)) {
                return null;
            }
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            log.debug("Refresh token validation failed: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String extractUsername(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("username", String.class);
        } catch (Exception e) {
            log.debug("Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    @Override
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
