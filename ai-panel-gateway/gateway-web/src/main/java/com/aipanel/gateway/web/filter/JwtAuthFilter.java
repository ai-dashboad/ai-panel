package com.aipanel.gateway.web.filter;

import com.aipanel.common.constant.CommonConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Global filter for JWT authentication
 * Extracts user ID from JWT token and sets X-User-Id header for downstream services
 */
@Component
@Slf4j
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // Paths that should use JWT authentication (not API key)
    private static final List<String> JWT_AUTH_PATHS = List.of(
        "/api/v1/users",
        "/api/v1/keys",
        "/api/v1/billing",
        "/api/v1/usage",
        "/api/v1/admin"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Only process JWT auth paths
        if (!isJwtAuthPath(path)) {
            return chain.filter(exchange);
        }

        // Extract JWT token from Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(CommonConstants.BEARER_PREFIX)) {
            // No JWT token, let downstream service handle it
            return chain.filter(exchange);
        }

        String token = authHeader.substring(CommonConstants.BEARER_PREFIX.length());

        try {
            // Validate JWT and extract user ID
            Long userId = validateAndExtractUserId(token);
            if (userId != null) {
                // Add user ID header for downstream services
                ServerHttpRequest modifiedRequest = request.mutate()
                    .header(CommonConstants.HEADER_USER_ID, String.valueOf(userId))
                    .build();
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            }
        } catch (Exception e) {
            log.debug("JWT validation failed: {}", e.getMessage());
        }

        // Continue without user ID header if JWT validation fails
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -110; // Run before ApiKeyAuthFilter
    }

    private boolean isJwtAuthPath(String path) {
        return JWT_AUTH_PATHS.stream().anyMatch(path::startsWith);
    }

    private Long validateAndExtractUserId(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            // Check if it's an access token
            String type = claims.get("type", String.class);
            if (!"access".equals(type)) {
                log.debug("Not an access token");
                return null;
            }

            // Extract user ID from subject
            String subject = claims.getSubject();
            if (subject != null) {
                return Long.parseLong(subject);
            }
        } catch (Exception e) {
            log.debug("Failed to parse JWT: {}", e.getMessage());
        }
        return null;
    }
}
