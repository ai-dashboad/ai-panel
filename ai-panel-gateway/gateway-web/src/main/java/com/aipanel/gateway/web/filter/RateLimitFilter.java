package com.aipanel.gateway.web.filter;

import com.aipanel.common.constant.CommonConstants;
import com.aipanel.common.domain.R;
import com.aipanel.common.enums.ErrorCode;
import com.aipanel.common.util.JsonUtils;
import com.aipanel.gateway.api.dto.RateLimitInfo;
import com.aipanel.gateway.core.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Global filter for rate limiting
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final RateLimitService rateLimitService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Skip rate limiting for health checks
        String path = request.getPath().value();
        if (path.startsWith("/actuator") || path.startsWith("/api/v1/health")) {
            return chain.filter(exchange);
        }

        // Check global rate limit first
        return rateLimitService.checkGlobalRateLimit()
            .flatMap(globalLimit -> {
                if (!globalLimit.isAllowed()) {
                    return rateLimitExceeded(exchange, globalLimit);
                }

                // Get user info from headers (set by ApiKeyAuthFilter)
                String userIdHeader = request.getHeaders().getFirst(CommonConstants.HEADER_USER_ID);
                String planType = request.getHeaders().getFirst("X-Plan-Type");
                String keyId = request.getHeaders().getFirst(CommonConstants.HEADER_API_KEY);

                if (userIdHeader == null) {
                    // No user context, proceed with global limit only
                    return addRateLimitHeaders(exchange, globalLimit, chain);
                }

                Long userId = Long.parseLong(userIdHeader);

                // Check user rate limit
                return rateLimitService.checkUserRateLimit(userId, planType)
                    .flatMap(userLimit -> {
                        if (!userLimit.isAllowed()) {
                            return rateLimitExceeded(exchange, userLimit);
                        }

                        // Check API key rate limit
                        return rateLimitService.checkApiKeyRateLimit(keyId, null)
                            .flatMap(keyLimit -> {
                                if (!keyLimit.isAllowed()) {
                                    return rateLimitExceeded(exchange, keyLimit);
                                }

                                // Use the most restrictive limit for headers
                                RateLimitInfo mostRestrictive = getMostRestrictive(globalLimit, userLimit, keyLimit);
                                return addRateLimitHeaders(exchange, mostRestrictive, chain);
                            });
                    });
            })
            .onErrorResume(e -> {
                log.error("Rate limit check error", e);
                // On error, allow request but log warning
                return chain.filter(exchange);
            });
    }

    @Override
    public int getOrder() {
        return -90; // Run after API Key auth filter
    }

    private RateLimitInfo getMostRestrictive(RateLimitInfo... limits) {
        RateLimitInfo mostRestrictive = null;
        for (RateLimitInfo limit : limits) {
            if (mostRestrictive == null || limit.getRemaining() < mostRestrictive.getRemaining()) {
                mostRestrictive = limit;
            }
        }
        return mostRestrictive;
    }

    private Mono<Void> addRateLimitHeaders(ServerWebExchange exchange, RateLimitInfo info,
                                            GatewayFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add(CommonConstants.HEADER_RATE_LIMIT, String.valueOf(info.getLimit()));
        response.getHeaders().add(CommonConstants.HEADER_RATE_LIMIT_REMAINING, String.valueOf(info.getRemaining()));
        response.getHeaders().add(CommonConstants.HEADER_RATE_LIMIT_RESET, String.valueOf(info.getResetAt()));

        return chain.filter(exchange);
    }

    private Mono<Void> rateLimitExceeded(ServerWebExchange exchange, RateLimitInfo info) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().add(CommonConstants.HEADER_RATE_LIMIT, String.valueOf(info.getLimit()));
        response.getHeaders().add(CommonConstants.HEADER_RATE_LIMIT_REMAINING, "0");
        response.getHeaders().add(CommonConstants.HEADER_RATE_LIMIT_RESET, String.valueOf(info.getResetAt()));
        response.getHeaders().add(CommonConstants.HEADER_RETRY_AFTER, String.valueOf(info.getSecondsUntilReset()));

        String message = String.format("Rate limit exceeded. Retry after %d seconds.", info.getSecondsUntilReset());
        R<?> errorResponse = R.fail(ErrorCode.RATE_LIMIT_EXCEEDED, message);
        String json = JsonUtils.toJson(errorResponse);
        byte[] bytes = json != null ? json.getBytes(StandardCharsets.UTF_8) : "{}".getBytes(StandardCharsets.UTF_8);

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
