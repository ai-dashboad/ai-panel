package com.aipanel.dispatch.core.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

/**
 * Checks per-employee monthly usage quota before a request is dispatched to a provider.
 * Calls the ai-panel-billing Cloudflare Worker's /quota-check endpoint directly — that
 * service isn't Eureka-registered, so this is a plain WebClient call, not a Feign client.
 */
@Service
@Slf4j
public class BillingQuotaService {

    @Value("${billing.quota-check-url}")
    private String quotaCheckUrl;

    @Value("${billing.quota-check-timeout-ms:3000}")
    private long timeoutMs;

    /** Shared secret expected by the billing worker's internal endpoints (X-Internal-Token). */
    @Value("${billing.internal-token:}")
    private String internalToken;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder().build();
    }

    /**
     * Returns the quota check result for a user, or empty if the check couldn't be
     * completed (billing service unreachable/erroring). Callers should treat empty
     * as "allow" — a billing outage shouldn't block every employee's requests.
     */
    public Mono<QuotaCheckResult> checkQuota(Long userId) {
        if (userId == null) {
            return Mono.empty();
        }

        return webClient.get()
            .uri(quotaCheckUrl + "?userId=" + userId)
            .headers(h -> {
                if (internalToken != null && !internalToken.isBlank()) {
                    h.set("X-Internal-Token", internalToken);
                }
            })
            .retrieve()
            .bodyToMono(QuotaCheckEnvelope.class)
            .timeout(Duration.ofMillis(timeoutMs))
            .mapNotNull(envelope -> envelope.getData())
            .onErrorResume(e -> {
                log.warn("Quota check failed for userId={}, allowing request through: {}", userId, e.getMessage());
                return Mono.empty();
            });
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuotaCheckEnvelope {
        private int code;
        private String message;
        private QuotaCheckResult data;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuotaCheckResult {
        private boolean allowed;
        private long used;
        private long limit;
        private long remaining;
        private long periodStart;
        private long periodEnd;
    }
}
