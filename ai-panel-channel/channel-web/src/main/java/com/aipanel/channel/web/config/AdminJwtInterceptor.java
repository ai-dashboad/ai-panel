package com.aipanel.channel.web.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Protects the channel admin APIs ({@code /api/v1/providers/**}, {@code /api/v1/channels/**}).
 * <p>
 * The channel service can be reached directly (the UI's nginx proxies {@code /api/v1/providers}
 * to it, and the dev compose publishes its port), so the JWT is verified here rather than
 * trusting gateway headers. Requires an access token with {@code role=ADMIN}; when
 * {@code JWT_SECRET} is not configured every admin request is rejected.
 */
@Slf4j
@Component
public class AdminJwtInterceptor implements HandlerInterceptor {

    private final SecretKey signingKey;

    public AdminJwtInterceptor(@Value("${aipanel.security.jwt-secret:}") String jwtSecret) {
        SecretKey key = null;
        if (jwtSecret != null && !jwtSecret.isBlank()) {
            byte[] bytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            if (bytes.length >= 32) {
                key = Keys.hmacShaKeyFor(bytes);
            } else {
                log.error("JWT_SECRET is shorter than 32 bytes; channel admin APIs are disabled");
            }
        } else {
            log.warn("JWT_SECRET is not set; channel admin APIs (/api/v1/providers, /api/v1/channels) are disabled");
        }
        this.signingKey = key;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (signingKey == null) {
            return reject(response, 503, "Admin API disabled: JWT_SECRET is not configured on ai-panel-channel");
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return reject(response, 401, "Missing bearer token");
        }
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(auth.substring(7).trim()).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return reject(response, 401, "Invalid or expired token");
        }
        if (!"access".equals(claims.get("type", String.class))) {
            return reject(response, 401, "Invalid token type");
        }
        String role = claims.get("role", String.class);
        if (role == null || !"ADMIN".equals(role.trim().toUpperCase(Locale.ROOT))) {
            return reject(response, 403, "Admin role required");
        }
        return true;
    }

    private static boolean reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String safe = message.replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + safe + "\",\"data\":null}");
        return false;
    }
}
