package com.aipanel.gateway.web.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthFilterTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-bytes-long!!";
    private final JwtAuthFilter filter = new JwtAuthFilter(SECRET);

    /** Records the exchange forwarded downstream (null if the filter short-circuited). */
    private static final class RecordingChain implements GatewayFilterChain {
        final AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            forwarded.set(exchange);
            return Mono.empty();
        }
    }

    private static String token(String type, String role, String secret, long ttlMillis) {
        var builder = Jwts.builder()
            .subject("42")
            .claim("username", "alice")
            .claim("type", type)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + ttlMillis))
            .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)));
        if (role != null) {
            builder.claim("role", role);
        }
        return builder.compact();
    }

    private static String token(String role) {
        return token("access", role, SECRET, 60_000);
    }

    private record Result(MockServerWebExchange exchange, ServerWebExchange forwarded) {
        HttpStatus status() {
            return (HttpStatus) exchange.getResponse().getStatusCode();
        }

        String forwardedHeader(String name) {
            return forwarded.getRequest().getHeaders().getFirst(name);
        }
    }

    private Result run(MockServerHttpRequest request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        RecordingChain chain = new RecordingChain();
        filter.filter(exchange, chain).block();
        return new Result(exchange, chain.forwarded.get());
    }

    private static MockServerHttpRequest.BaseBuilder<?> get(String path, String bearer) {
        MockServerHttpRequest.BaseBuilder<?> b = MockServerHttpRequest.get(path);
        if (bearer != null) {
            b.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
        }
        return b;
    }

    @Test
    void adminPathWithoutTokenIs401() {
        Result r = run(get("/api/v1/admin/orders", null).build());
        assertThat(r.forwarded()).isNull();
        assertThat(r.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void adminPathWithInvalidOrExpiredTokenIs401() {
        assertThat(run(get("/api/v1/admin/orders", token("access", "ADMIN", "another-secret-that-is-also-32-bytes-long!!", 60_000)).build()).status())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(run(get("/api/v1/admin/orders", token("access", "ADMIN", SECRET, -60_000)).build()).status())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(run(get("/api/v1/admin/orders", token("refresh", "ADMIN", SECRET, 60_000)).build()).status())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void adminPathWithNonAdminTokenIs403() {
        assertThat(run(get("/api/v1/providers", token("USER")).build()).status()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(run(get("/api/v1/channels", null).build()).status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        Result user = run(get("/api/v1/admin/platforms", token("USER")).build());
        assertThat(user.forwarded()).isNull();
        assertThat(user.status()).isEqualTo(HttpStatus.FORBIDDEN);

        // Tokens issued before the role claim existed are treated as USER.
        assertThat(run(get("/api/v1/admin/skus", token(null)).build()).status()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminPathWithAdminTokenIsForwardedWithIdentity() {
        Result r = run(get("/api/v1/admin/orders", token("admin")).header("X-User-ID", "1").build());
        assertThat(r.forwarded()).isNotNull();
        assertThat(r.forwardedHeader("X-User-ID")).isEqualTo("42");
        assertThat(r.forwardedHeader(JwtAuthFilter.HEADER_USER_ROLE)).isEqualTo("ADMIN");
    }

    @Test
    void encodedOrPaddedAdminPathsAreStillProtected() {
        // URI.create keeps the percent-encoding as sent on the wire (the String overload would re-encode '%').
        MockServerHttpRequest encoded = MockServerHttpRequest.method(HttpMethod.GET, URI.create("/api/v1/%61dmin/orders"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("USER")).build();
        assertThat(run(encoded).status()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(run(get("http://localhost//api/v1/admin;x=1/orders", token("USER")).build()).status()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(run(get("/api/v1/admin", null).build()).status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(run(get("/api/v1/users/../admin/orders", token("USER")).build()).status()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void spoofedIdentityHeadersAreStripped() {
        Result noToken = run(get("/api/v1/users/5", null)
            .header("X-User-ID", "5").header("X-User-Role", "ADMIN").header("X-Plan-Type", "ENTERPRISE").build());
        assertThat(noToken.forwarded()).isNotNull();
        assertThat(noToken.forwardedHeader("X-User-ID")).isNull();
        assertThat(noToken.forwardedHeader("X-User-Role")).isNull();
        assertThat(noToken.forwardedHeader("X-Plan-Type")).isNull();

        Result withToken = run(get("/api/v1/users/me", token("USER")).header("X-User-ID", "5").build());
        assertThat(withToken.forwardedHeader("X-User-ID")).isEqualTo("42");
        assertThat(withToken.forwardedHeader("X-User-Role")).isEqualTo("USER");

        Result apiKeyPath = run(get("/api/v1/chat/completions", null).header("X-User-ID", "5").build());
        assertThat(apiKeyPath.forwardedHeader("X-User-ID")).isNull();
    }

    @Test
    void internalEndpointsAreNotExposed() {
        assertThat(run(get("/api/v1/users/internal/1", null).build()).status()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(run(get("/api/v1/keys/internal/validate", null).build()).status()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void preflightOnAdminPathIsNotBlocked() {
        Result r = run(MockServerHttpRequest.options("/api/v1/admin/orders").build());
        assertThat(r.forwarded()).isNotNull();
    }

    @Test
    void lookalikePrefixesAreNotTreatedAsAdmin() {
        assertThat(JwtAuthFilter.isAdminPath("/api/v1/administrator")).isFalse();
        assertThat(JwtAuthFilter.isAdminPath("/api/v1/admin/x")).isTrue();
        assertThat(JwtAuthFilter.isAdminPath("/api/v1/providers")).isTrue();
        assertThat(JwtAuthFilter.isAdminPath("/api/v1/providers/3/models")).isTrue();
        assertThat(JwtAuthFilter.isAdminPath("/api/v1/channels/keys/generate")).isTrue();
        assertThat(JwtAuthFilter.isAdminPath("/api/v1/providersx")).isFalse();
    }
}
