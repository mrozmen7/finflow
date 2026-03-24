package com.finflow.shared.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String KEY_PREFIX = "idempotency:";
    private static final long TTL_HOURS = 24;

    private final RedisTemplate<String, String> redisTemplate;

    public IdempotencyService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Returns the transaction ID previously stored for this idempotency key, if any.
     */
    public Optional<UUID> get(String idempotencyKey) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + idempotencyKey);
        if (value == null) {
            return Optional.empty();
        }
        log.debug("Idempotency hit for key: {}", idempotencyKey);
        return Optional.of(UUID.fromString(value));
    }

    /**
     * Stores the transaction ID for this idempotency key with a 24-hour TTL.
     */
    public void store(String idempotencyKey, UUID transactionId) {
        redisTemplate.opsForValue().set(
            KEY_PREFIX + idempotencyKey,
            transactionId.toString(),
            TTL_HOURS,
            TimeUnit.HOURS
        );
        log.debug("Idempotency key stored: {}, transactionId: {}", idempotencyKey, transactionId);
    }
}
