package com.aipanel.channel.web;

import com.aipanel.channel.web.config.AdminJwtInterceptor;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class AdminJwtInterceptorTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-0123456789";

    private static String token(String role, String type) {
        return Jwts.builder().subject("1").claim("role", role).claim("type", type)
            .expiration(new Date(System.currentTimeMillis() + 60_000))
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    private static int status(AdminJwtInterceptor interceptor, String authorization) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/providers");
        if (authorization != null) {
            req.addHeader("Authorization", authorization);
        }
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean ok = interceptor.preHandle(req, resp, new Object());
        return ok ? 200 : resp.getStatus();
    }

    @Test
    void failsClosedWithoutSecret() throws Exception {
        assertThat(status(new AdminJwtInterceptor(""), "Bearer " + token("ADMIN", "access"))).isEqualTo(503);
    }

    @Test
    void requiresAdminAccessToken() throws Exception {
        AdminJwtInterceptor i = new AdminJwtInterceptor(SECRET);
        assertThat(status(i, null)).isEqualTo(401);
        assertThat(status(i, "Bearer garbage")).isEqualTo(401);
        assertThat(status(i, "Bearer " + token("ADMIN", "refresh"))).isEqualTo(401);
        assertThat(status(i, "Bearer " + token("USER", "access"))).isEqualTo(403);
        assertThat(status(i, "Bearer " + token("ADMIN", "access"))).isEqualTo(200);
        assertThat(status(i, "Bearer " + token("admin", "access"))).isEqualTo(200);
    }
}
