package com.aipanel.gateway.web.config;

import com.aipanel.common.util.CorsOrigins;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS for the gateway, driven by {@code CORS_ALLOWED_ORIGINS} (comma-separated).
 * Only listed origins get CORS headers; requests from other origins are rejected by browsers.
 * Credentials (cookies) are not allowed: the API authenticates with bearer tokens / API keys.
 */
@Configuration
@Slf4j
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(@Value("${cors.allowed-origins:}") String allowedOrigins) {
        return new CorsWebFilter(buildSource(allowedOrigins));
    }

    static UrlBasedCorsConfigurationSource buildSource(String allowedOrigins) {
        List<String> origins = CorsOrigins.parse(allowedOrigins);
        if (origins.isEmpty()) {
            log.warn("CORS_ALLOWED_ORIGINS is empty: cross-origin browser requests will be rejected");
        } else if (CorsOrigins.allowsAny(origins)) {
            log.warn("CORS_ALLOWED_ORIGINS='*': any website can call this API from a browser");
        } else {
            log.info("CORS allowed origins: {}", origins);
        }

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-RateLimit-Limit", "X-RateLimit-Remaining", "X-RateLimit-Reset", "X-Request-ID"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
