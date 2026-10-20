package com.aipanel.gateway.data.service;

import com.aipanel.gateway.api.dto.ApiKeyValidationResult;
import com.aipanel.gateway.core.service.ApiKeyValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * API Key validation service implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyValidationServiceImpl implements ApiKeyValidationService {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final WebClient.Builder webClientBuilder;

    private static final String KEY_CACHE_PREFIX = "api_key:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    @Override
    public Mono<ApiKeyValidationResult> validate(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Mono.just(ApiKeyValidationResult.invalid(401, "API key is required"));
        }

        String keyHash = hashKey(apiKey);
        String cacheKey = KEY_CACHE_PREFIX + keyHash;

        // Try cache first
        return redisTemplate.opsForValue().get(cacheKey)
            .cast(ApiKeyValidationResult.class)
            .switchIfEmpty(validateFromKeyService(apiKey, cacheKey))
            .onErrorResume(e -> {
                log.error("Error validating API key: {}", e.getMessage());
                return Mono.just(ApiKeyValidationResult.invalid(500, "Validation service error"));
            });
    }

    @Override
    public Mono<ApiKeyValidationResult> validateWithModel(String apiKey, String model) {
        return validate(apiKey)
            .map(result -> {
                if (result.isValid() && !result.isModelAllowed(model)) {
                    return ApiKeyValidationResult.invalid(403, "Model not allowed for this API key: " + model);
                }
                return result;
            });
    }

    @Override
    public void invalidateCache(String keyId) {
        String cacheKey = KEY_CACHE_PREFIX + keyId;
        redisTemplate.delete(cacheKey).subscribe();
        log.info("Invalidated cache for key: {}", keyId);
    }

    private Mono<ApiKeyValidationResult> validateFromKeyService(String apiKey, String cacheKey) {
        // Call key service to validate
        return webClientBuilder.build()
            .get()
            .uri("http://ai-panel-key:8085/api/v1/keys/validate?key={key}", apiKey)
            .retrieve()
            .bodyToMono(String.class)
            .flatMap(responseBody -> {
                try {
                    // Parse R<ApiKeyValidationResponse> wrapper
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(responseBody);

                    int code = root.path("code").asInt();
                    if (code != 200) {
                        String message = root.path("message").asText("Validation failed");
                        return Mono.just(ApiKeyValidationResult.invalid(code, message));
                    }

                    JsonNode data = root.path("data");
                    if (data.isMissingNode() || !data.path("valid").asBoolean()) {
                        int errorCode = data.path("errorCode").asInt(401);
                        String errorMessage = data.path("errorMessage").asText("Invalid API key");
                        return Mono.just(ApiKeyValidationResult.invalid(errorCode, errorMessage));
                    }

                    // Build valid result
                    List<String> allowedModels = new ArrayList<>();
                    JsonNode modelsNode = data.path("allowedModels");
                    if (modelsNode.isArray()) {
                        for (JsonNode model : modelsNode) {
                            allowedModels.add(model.asText());
                        }
                    }

                    ApiKeyValidationResult result = ApiKeyValidationResult.valid(
                        data.path("userId").asLong(),
                        data.path("keyId").asText(),
                        data.path("planType").asText(),
                        allowedModels,
                        data.path("rateLimitPerMinute").asInt(60)
                    );

                    // Cache valid results
                    return redisTemplate.opsForValue()
                        .set(cacheKey, result, CACHE_TTL)
                        .thenReturn(result);
                } catch (Exception e) {
                    log.error("Failed to parse key service response: {}", e.getMessage());
                    return Mono.just(ApiKeyValidationResult.invalid(500, "Failed to parse validation response"));
                }
            })
            .onErrorResume(e -> {
                log.warn("Key service unavailable, attempting local validation: {}", e.getMessage());
                // Fallback: for development, accept keys starting with "sk-"
                if (apiKey.startsWith("sk-") && apiKey.length() >= 32) {
                    return Mono.just(ApiKeyValidationResult.valid(
                        1L,
                        "dev-key",
                        "PRO",
                        List.of("gpt-4", "gpt-3.5-turbo", "claude-3-opus", "claude-3-sonnet"),
                        60
                    ));
                }
                return Mono.just(ApiKeyValidationResult.invalid(401, "Invalid API key"));
            });
    }

    private String hashKey(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return apiKey.substring(0, Math.min(16, apiKey.length()));
        }
    }
}
