package com.aipanel.gateway.web.filter;

import com.aipanel.common.constant.CommonConstants;
import com.aipanel.common.domain.R;
import com.aipanel.common.enums.ErrorCode;
import com.aipanel.common.util.JsonUtils;
import com.aipanel.gateway.api.dto.ApiKeyValidationResult;
import com.aipanel.gateway.core.service.ApiKeyValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Global filter for API Key authentication
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthFilter implements GlobalFilter, Ordered {

    private final ApiKeyValidationService apiKeyValidationService;

    // Paths that don't require API key authentication (may use JWT instead)
    private static final List<String> WHITE_LIST = List.of(
        "/api/v1/auth",    // Auth endpoints (login, register, refresh)
        "/api/v1/users",   // User endpoints (use JWT auth)
        "/api/v1/keys",    // API Key management endpoints (use JWT auth)
        "/api/v1/billing", // Billing endpoints (use JWT auth)
        "/api/v1/usage",   // Usage endpoints (use JWT auth)
        "/api/v1/admin",   // Admin endpoints (use JWT auth)
        "/api/v1/health",
        "/actuator",
        "/swagger-ui",
        "/v3/api-docs",
        "/api-docs"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Skip authentication for whitelisted paths
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // Extract API key from Authorization header
        String apiKey = extractApiKey(request);
        if (apiKey == null) {
            return unauthorized(exchange, "Missing API Key");
        }

        // Validate API key
        return apiKeyValidationService.validate(apiKey)
            .flatMap(result -> {
                if (!result.isValid()) {
                    return forbidden(exchange, result.getErrorMessage());
                }

                // Add user info to request headers for downstream services
                ServerHttpRequest modifiedRequest = request.mutate()
                    .header(CommonConstants.HEADER_USER_ID, String.valueOf(result.getUserId()))
                    .header(CommonConstants.HEADER_API_KEY, result.getKeyId())
                    .header("X-Plan-Type", result.getPlanType())
                    .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            })
            .onErrorResume(e -> {
                log.error("API Key validation error", e);
                return serverError(exchange, "Authentication service error");
            });
    }

    @Override
    public int getOrder() {
        return -100; // Run early in the filter chain
    }

    private String extractApiKey(ServerHttpRequest request) {
        // Try Authorization header first (Bearer token)
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(CommonConstants.BEARER_PREFIX)) {
            return authHeader.substring(CommonConstants.BEARER_PREFIX.length());
        }

        // Try X-API-Key header
        String apiKeyHeader = request.getHeaders().getFirst(CommonConstants.HEADER_API_KEY);
        if (apiKeyHeader != null) {
            return apiKeyHeader;
        }

        return null;
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, message);
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        return writeErrorResponse(exchange, HttpStatus.FORBIDDEN, ErrorCode.API_KEY_INVALID, message);
    }

    private Mono<Void> serverError(ServerWebExchange exchange, String message) {
        return writeErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, message);
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status,
                                          ErrorCode errorCode, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        R<?> errorResponse = R.fail(errorCode, message);
        String json = JsonUtils.toJson(errorResponse);
        byte[] bytes = json != null ? json.getBytes(StandardCharsets.UTF_8) : "{}".getBytes(StandardCharsets.UTF_8);

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
