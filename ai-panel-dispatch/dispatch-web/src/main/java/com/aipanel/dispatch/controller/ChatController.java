package com.aipanel.dispatch.controller;

import com.aipanel.common.domain.R;
import com.aipanel.common.enums.ErrorCode;
import com.aipanel.common.exception.BusinessException;
import com.aipanel.common.util.ApiKeyGenerator;
import com.aipanel.dispatch.api.dto.request.ChatRequest;
import com.aipanel.dispatch.api.dto.response.ChatResponse;
import com.aipanel.dispatch.core.service.BillingQuotaService;
import com.aipanel.dispatch.core.service.ModelRouterService;
import com.aipanel.dispatch.data.po.RequestLogPO;
import com.aipanel.dispatch.data.service.RequestLogDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Chat Completion Controller
 * OpenAI-compatible chat completion endpoints
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Chat", description = "Chat Completion API (OpenAI Compatible)")
@Slf4j
@RequiredArgsConstructor
public class ChatController {

    private final ModelRouterService modelRouterService;
    private final RequestLogDataService requestLogDataService;
    private final BillingQuotaService billingQuotaService;

    /**
     * Create chat completion
     */
    @PostMapping("/chat/completions")
    @Operation(summary = "Create chat completion", description = "Creates a model response for the given chat conversation")
    public Mono<ChatResponse> createChatCompletion(
            @Valid @RequestBody ChatRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Api-Key-Id", required = false) Long apiKeyId,
            HttpServletRequest httpRequest
    ) {
        String requestId = ApiKeyGenerator.generateResponseId();
        long startTime = System.currentTimeMillis();

        log.info("Chat request: requestId={}, model={}, userId={}", requestId, request.getModel(), userId);

        return checkQuota(userId)
            .then(modelRouterService.chat(request))
            .doOnSuccess(response -> {
                long latency = System.currentTimeMillis() - startTime;
                logRequest(requestId, userId, apiKeyId, request.getModel(),
                    response.getModel(), "chat", response.getUsage(), latency, "success", null, httpRequest);
            })
            .doOnError(e -> {
                long latency = System.currentTimeMillis() - startTime;
                logRequest(requestId, userId, apiKeyId, request.getModel(),
                    null, "chat", null, latency, "error", e.getMessage(), httpRequest);
            });
    }

    /**
     * Create streaming chat completion
     */
    @PostMapping(value = "/chat/completions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Create streaming chat completion", description = "Creates a streaming model response")
    public Flux<ChatResponse> createStreamingChatCompletion(
            @Valid @RequestBody ChatRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Api-Key-Id", required = false) Long apiKeyId,
            HttpServletRequest httpRequest
    ) {
        String requestId = ApiKeyGenerator.generateResponseId();
        long startTime = System.currentTimeMillis();

        log.info("Streaming chat request: requestId={}, model={}, userId={}", requestId, request.getModel(), userId);

        request.setStream(true);

        return checkQuota(userId)
            .thenMany(modelRouterService.chatStream(request))
            .doOnComplete(() -> {
                long latency = System.currentTimeMillis() - startTime;
                logRequest(requestId, userId, apiKeyId, request.getModel(),
                    request.getModel(), "stream", null, latency, "success", null, httpRequest);
            })
            .doOnError(e -> {
                long latency = System.currentTimeMillis() - startTime;
                logRequest(requestId, userId, apiKeyId, request.getModel(),
                    null, "stream", null, latency, "error", e.getMessage(), httpRequest);
            });
    }

    /**
     * Get health status of all providers
     */
    @GetMapping("/providers/health")
    @Operation(summary = "Get provider health status", description = "Returns health status of all AI providers")
    public R<java.util.Map<String, Boolean>> getProviderHealth() {
        return R.ok(modelRouterService.getProviderHealth());
    }

    /**
     * Rejects the request if the user has exceeded their monthly token quota.
     * Completes empty (allowing the request through) when userId is absent or the
     * billing service check couldn't be completed, so a billing outage doesn't block
     * every employee's requests.
     */
    private Mono<Void> checkQuota(Long userId) {
        return billingQuotaService.checkQuota(userId)
            .flatMap(quota -> {
                if (!quota.isAllowed()) {
                    return Mono.error(BusinessException.of(ErrorCode.INSUFFICIENT_BALANCE,
                        String.format("Monthly quota exceeded (%d/%d tokens used)", quota.getUsed(), quota.getLimit())));
                }
                return Mono.empty();
            });
    }

    private void logRequest(String requestId, Long userId, Long apiKeyId,
                            String requestedModel, String actualModel, String requestType,
                            ChatResponse.Usage usage, long latencyMs, String status,
                            String errorMessage, HttpServletRequest httpRequest) {
        try {
            RequestLogPO log = new RequestLogPO();
            log.setRequestId(requestId);
            log.setUserId(userId);
            log.setApiKeyId(apiKeyId);
            log.setRequestedModel(requestedModel);
            log.setActualModel(actualModel);
            log.setRequestType(requestType);

            if (usage != null) {
                log.setInputTokens(usage.getPromptTokens());
                log.setOutputTokens(usage.getCompletionTokens());
                log.setTotalTokens(usage.getTotalTokens());
            }

            log.setLatencyMs(latencyMs);
            log.setStatus(status);
            log.setErrorMessage(errorMessage);
            log.setClientIp(getClientIp(httpRequest));
            log.setUserAgent(httpRequest.getHeader("User-Agent"));
            log.setFallbackUsed(!requestedModel.equals(actualModel) && actualModel != null);
            log.setRetryCount(0);

            requestLogDataService.queueLog(log);
        } catch (Exception e) {
            // Don't let logging failures affect the response
            this.log.error("Failed to log request: {}", e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
