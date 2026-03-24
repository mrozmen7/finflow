package com.finflow.shared.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);
    private static final String KEY_PREFIX = "rate_limit:transfer:";
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long WINDOW_SECONDS = 60;

    private final RedisTemplate<String, String> redisTemplate;

    public RateLimiterService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Returns true if the account is within the rate limit, false if it is exceeded.
     * Uses Redis INCR + EXPIRE to implement a fixed window counter per account per minute.
     */
    public boolean isAllowed(UUID accountId) {
        String key = KEY_PREFIX + accountId.toString();

        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            return true;
        }

        if (count == 1) {
            redisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
        }

        boolean allowed = count <= MAX_REQUESTS_PER_MINUTE;
        if (!allowed) {
            log.warn("Rate limit exceeded for account: {}, count: {}", accountId, count);
        }
        return allowed;
    }
}
