package com.aipanel.gateway.data.redis;

import com.aipanel.common.constant.CommonConstants;
import com.aipanel.common.enums.PlanType;
import com.aipanel.gateway.api.dto.RateLimitInfo;
import com.aipanel.gateway.core.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Rate limiting service implementation using Redis + Lua script (Token Bucket)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitServiceImpl implements RateLimitService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    // Global rate limit: 10000 RPM
    private static final long GLOBAL_RATE_LIMIT = 10000;

    // Model-specific rate limits
    private static final long GPT4_RATE_LIMIT = 60;
    private static final long CLAUDE_RATE_LIMIT = 100;
    private static final long DEFAULT_MODEL_RATE_LIMIT = 300;

    // Token bucket Lua script
    private static final String TOKEN_BUCKET_SCRIPT = """
        local key = KEYS[1]
        local capacity = tonumber(ARGV[1])
        local rate = tonumber(ARGV[2])
        local now = tonumber(ARGV[3])
        local requested = tonumber(ARGV[4])

        local bucket = redis.call('HMGET', key, 'tokens', 'last_time')
        local tokens = tonumber(bucket[1]) or capacity
        local last_time = tonumber(bucket[2]) or now

        local delta = math.max(0, now - last_time)
        local filled = math.min(capacity, tokens + delta * rate / 60)

        local allowed = filled >= requested
        if allowed then
            filled = filled - requested
        end

        redis.call('HMSET', key, 'tokens', filled, 'last_time', now)
        redis.call('EXPIRE', key, math.ceil(capacity / rate) * 120)

        local reset_at = now + math.ceil((capacity - filled) / rate * 60)
        return { allowed and 1 or 0, math.floor(filled), reset_at }
        """;

    @Override
    public Mono<RateLimitInfo> checkGlobalRateLimit() {
        String key = CommonConstants.CACHE_RATE_LIMIT + "global";
        return executeRateLimitScript(key, GLOBAL_RATE_LIMIT, GLOBAL_RATE_LIMIT, "global");
    }

    @Override
    public Mono<RateLimitInfo> checkUserRateLimit(Long userId, String planType) {
        PlanType plan = PlanType.fromCode(planType);
        long limit = plan.getRequestsPerMinute();
        String key = CommonConstants.CACHE_RATE_LIMIT + "user:" + userId;
        return executeRateLimitScript(key, limit, limit, "user");
    }

    @Override
    public Mono<RateLimitInfo> checkApiKeyRateLimit(String keyId, Integer customLimit) {
        long limit = customLimit != null ? customLimit : PlanType.BASIC.getRequestsPerMinute();
        String key = CommonConstants.CACHE_RATE_LIMIT + "key:" + keyId;
        return executeRateLimitScript(key, limit, limit, "api_key");
    }

    @Override
    public Mono<RateLimitInfo> checkModelRateLimit(String model) {
        long limit = getModelRateLimit(model);
        String key = CommonConstants.CACHE_RATE_LIMIT + "model:" + model;
        return executeRateLimitScript(key, limit, limit, "model");
    }

    @Override
    public Mono<RateLimitInfo> getRateLimitInfo(String key) {
        String fullKey = CommonConstants.CACHE_RATE_LIMIT + key;
        return redisTemplate.opsForHash().multiGet(fullKey, Arrays.asList("tokens", "last_time"))
            .map(values -> {
                long tokens = values.get(0) != null ? Long.parseLong(values.get(0).toString()) : 0;
                long lastTime = values.get(1) != null ? Long.parseLong(values.get(1).toString()) : 0;
                return RateLimitInfo.builder()
                    .remaining(tokens)
                    .resetAt(lastTime + 60)
                    .build();
            });
    }

    private Mono<RateLimitInfo> executeRateLimitScript(String key, long capacity, long rate, String limitType) {
        long now = System.currentTimeMillis() / 1000;

        RedisScript<List> script = RedisScript.of(TOKEN_BUCKET_SCRIPT, List.class);

        List<String> keys = List.of(key);
        List<String> args = List.of(
            String.valueOf(capacity),
            String.valueOf(rate),
            String.valueOf(now),
            "1"
        );

        return redisTemplate.execute(script, keys, args)
            .next()
            .map(result -> {
                @SuppressWarnings("unchecked")
                List<Long> resultList = (List<Long>) result;
                boolean allowed = resultList.get(0) == 1;
                long remaining = resultList.get(1);
                long resetAt = resultList.get(2);

                return RateLimitInfo.builder()
                    .limit(capacity)
                    .remaining(remaining)
                    .resetAt(resetAt)
                    .limitType(limitType)
                    .build();
            })
            .doOnNext(info -> {
                if (!info.isAllowed()) {
                    log.warn("Rate limit exceeded for {}: {}", limitType, key);
                }
            });
    }

    private long getModelRateLimit(String model) {
        if (model == null) {
            return DEFAULT_MODEL_RATE_LIMIT;
        }

        String lowerModel = model.toLowerCase();
        if (lowerModel.contains("gpt-4") || lowerModel.contains("gpt4")) {
            return GPT4_RATE_LIMIT;
        }
        if (lowerModel.contains("claude")) {
            return CLAUDE_RATE_LIMIT;
        }
        return DEFAULT_MODEL_RATE_LIMIT;
    }
}
