package com.aipanel.user.core.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceImplTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-bytes-long!!";

    private JwtServiceImpl service() {
        JwtServiceImpl s = new JwtServiceImpl();
        ReflectionTestUtils.setField(s, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(s, "accessTokenExpiration", 3600L);
        ReflectionTestUtils.setField(s, "refreshTokenExpiration", 604800L);
        return s;
    }

    private static Claims parse(String token) {
        return Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
            .build().parseSignedClaims(token).getPayload();
    }

    @Test
    void accessTokenCarriesNormalizedRoleClaim() {
        JwtServiceImpl s = service();
        assertThat(parse(s.generateAccessToken(1L, "admin", "admin")).get("role")).isEqualTo("ADMIN");
        assertThat(parse(s.generateAccessToken(2L, "bob", "USER")).get("role")).isEqualTo("USER");
        assertThat(parse(s.generateAccessToken(3L, "eve", null)).get("role")).isEqualTo("USER");
        assertThat(parse(s.generateAccessToken(4L, "mallory", "root")).get("role")).isEqualTo("USER");
    }

    @Test
    void accessTokenStillValidates() {
        JwtServiceImpl s = service();
        String token = s.generateAccessToken(9L, "alice", "USER");
        assertThat(s.validateAccessToken(token)).isEqualTo(9L);
        assertThat(s.validateRefreshToken(token)).isNull();
    }
}
