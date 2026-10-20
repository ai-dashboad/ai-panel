package com.aipanel.gateway.web.filter;

import com.aipanel.common.constant.CommonConstants;
import com.aipanel.common.domain.R;
import com.aipanel.common.enums.ErrorCode;
import com.aipanel.common.util.JsonUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Global JWT filter. Runs before {@link ApiKeyAuthFilter}.
 * <ol>
 *   <li>Strips identity headers ({@code X-User-ID}, {@code X-User-Role}, {@code X-Plan-Type}) supplied by
 *       the client on every request, so downstream services only ever see values set by the gateway.</li>
 *   <li>Rejects {@code /api/v1/.../internal/...} paths (service-to-service endpoints) with 404.</li>
 *   <li>For JWT paths, validates the access token and forwards {@code X-User-ID} / {@code X-User-Role}.</li>
 *   <li>For admin paths ({@code /api/v1/admin/**}, {@code /api/v1/providers/**}, {@code /api/v1/channels/**}): missing/invalid token -> 401, token without {@code role=ADMIN} -> 403.</li>
 * </ol>
 */
@Component
@Slf4j
public class JwtAuthFilter implements GlobalFilter, Ordered {

    public static final String HEADER_USER_ROLE = "X-User-Role";
    static final String HEADER_PLAN_TYPE = "X-Plan-Type";
    static final String ADMIN_PATH_PREFIX = "/api/v1/admin";
    /** Admin-only path prefixes: user-service admin APIs and channel-service provider/channel management. */
    static final List<String> ADMIN_PATH_PREFIXES = List.of(
        ADMIN_PATH_PREFIX, "/api/v1/providers", "/api/v1/channels"
    );
    static final String ROLE_ADMIN = "ADMIN";

    /** Headers that only the gateway may set; any client-supplied value is removed. */
    private static final List<String> IDENTITY_HEADERS = List.of(
        CommonConstants.HEADER_USER_ID, HEADER_USER_ROLE, HEADER_PLAN_TYPE
    );

    // Paths that should use JWT authentication (not API key)
    private static final List<String> JWT_AUTH_PATHS = List.of(
        "/api/v1/users",
        "/api/v1/keys",
        "/api/v1/billing",
        "/api/v1/usage",
        ADMIN_PATH_PREFIX,
        "/api/v1/providers",
        "/api/v1/channels"
    );

    private final SecretKey signingKey;

    public JwtAuthFilter(@Value("${jwt.secret}") String jwtSecret) {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /** Verified identity extracted from an access token. */
    record TokenIdentity(Long userId, String role) {
        boolean isAdmin() {
            return ROLE_ADMIN.equals(role);
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest()
            .mutate()
            .headers(h -> IDENTITY_HEADERS.forEach(h::remove))
            .build();
        exchange = exchange.mutate().request(request).build();
        String path = normalizePath(request.getPath().pathWithinApplication());
        if (path == null) {
            return writeError(exchange, HttpStatus.BAD_REQUEST, ErrorCode.PARAM_ERROR, "Invalid path");
        }

        if (isInternalPath(path)) {
            return writeError(exchange, HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Not found");
        }

        if (!isJwtAuthPath(path)) {
            return chain.filter(exchange);
        }

        boolean adminPath = isAdminPath(path);
        if (adminPath && HttpMethod.OPTIONS.equals(request.getMethod())) {
            return chain.filter(exchange); // CORS preflight carries no credentials
        }

        TokenIdentity identity = null;
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(CommonConstants.BEARER_PREFIX)) {
            identity = parseAccessToken(authHeader.substring(CommonConstants.BEARER_PREFIX.length()));
        }

        if (adminPath) {
            if (identity == null) {
                return writeError(exchange, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Authentication required");
            }
            if (!identity.isAdmin()) {
                log.warn("Rejected non-admin user {} on {} {}", identity.userId(), request.getMethod(), path);
                return writeError(exchange, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Admin role required");
            }
        }

        if (identity == null) {
            // No (valid) JWT: forward without identity headers; downstream services return 401 as needed.
            return chain.filter(exchange);
        }

        TokenIdentity id = identity;
        ServerHttpRequest withIdentity = request.mutate()
            .headers(h -> {
                h.set(CommonConstants.HEADER_USER_ID, String.valueOf(id.userId()));
                h.set(HEADER_USER_ROLE, id.role());
            })
            .build();
        return chain.filter(exchange.mutate().request(withIdentity).build());
    }

    @Override
    public int getOrder() {
        return -110; // Run before ApiKeyAuthFilter (-100) and RateLimitFilter (-90)
    }

    /**
     * Decodes segments, drops matrix parameters and empty segments, so that encoded or padded variants
     * (e.g. {@code /api/v1/%61dmin}, {@code //api/v1/admin;x=1/}) are classified the same way downstream
     * services will route them. Returns {@code null} for dot-segments, which are rejected outright.
     */
    static String normalizePath(PathContainer path) {
        StringBuilder sb = new StringBuilder();
        for (PathContainer.Element element : path.elements()) {
            if (element instanceof PathContainer.PathSegment segment) {
                String value = segment.valueToMatch();
                if (value.isEmpty()) {
                    continue;
                }
                if (".".equals(value) || "..".equals(value) || value.contains("/")) {
                    return null;
                }
                sb.append('/').append(value);
            }
        }
        return sb.length() == 0 ? "/" : sb.toString();
    }

    static boolean isJwtAuthPath(String path) {
        return JWT_AUTH_PATHS.stream().anyMatch(p -> path.equals(p) || path.startsWith(p + "/"));
    }

    static boolean isAdminPath(String path) {
        return ADMIN_PATH_PREFIXES.stream().anyMatch(p -> path.equals(p) || path.startsWith(p + "/"));
    }

    static boolean isInternalPath(String path) {
        return path.startsWith("/api/") && (path.contains("/internal/") || path.endsWith("/internal"));
    }

    /** Returns the verified identity, or {@code null} if the token is invalid, expired or not an access token. */
    TokenIdentity parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            if (!"access".equals(claims.get("type", String.class))) {
                log.debug("Not an access token");
                return null;
            }
            String subject = claims.getSubject();
            if (subject == null) {
                return null;
            }
            String role = claims.get("role", String.class);
            String normalizedRole = role != null && ROLE_ADMIN.equals(role.trim().toUpperCase(Locale.ROOT))
                ? ROLE_ADMIN : "USER";
            return new TokenIdentity(Long.parseLong(subject), normalizedRole);
        } catch (Exception e) {
            log.debug("Failed to parse JWT: {}", e.getMessage());
            return null;
        }
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, ErrorCode errorCode, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String json = JsonUtils.toJson(R.fail(errorCode, message));
        byte[] bytes = (json != null ? json : "{}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
