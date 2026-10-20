package com.aipanel.gateway.web.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.reactive.CorsWebFilter;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsConfigTest {

    private static MockServerWebExchange preflight(CorsWebFilter filter, String origin) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.options("http://gateway:8080/api/v1/admin/orders")
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization")
                .build());
        filter.filter(exchange, ex -> Mono.empty()).block();
        return exchange;
    }

    @Test
    void allowsOnlyConfiguredOrigins() {
        CorsWebFilter filter = new CorsWebFilter(CorsConfig.buildSource("https://panel.example.com, http://localhost:5173/"));

        MockServerWebExchange ok = preflight(filter, "https://panel.example.com");
        assertThat(ok.getResponse().getHeaders().getAccessControlAllowOrigin()).isEqualTo("https://panel.example.com");
        assertThat(ok.getResponse().getHeaders().getAccessControlAllowCredentials()).isFalse();

        assertThat(preflight(filter, "http://localhost:5173").getResponse().getHeaders().getAccessControlAllowOrigin())
            .isEqualTo("http://localhost:5173");

        MockServerWebExchange evil = preflight(filter, "https://evil.example.org");
        assertThat(evil.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(evil.getResponse().getHeaders().getAccessControlAllowOrigin()).isNull();
    }

    @Test
    void emptyAllowlistRejectsCrossOriginRequests() {
        CorsWebFilter filter = new CorsWebFilter(CorsConfig.buildSource(""));
        assertThat(preflight(filter, "http://localhost:5173").getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void rejectsMalformedOrigins() {
        assertThatThrownBy(() -> CorsConfig.buildSource("panel.example.com"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CorsConfig.buildSource("*,https://panel.example.com"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
