package com.finflow.transaction.unit;

import com.finflow.shared.util.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("should return empty when idempotency key has not been processed before")
    void should_ReturnEmpty_When_KeyDoesNotExist() {
        when(valueOperations.get("idempotency:new-key")).thenReturn(null);

        Optional<UUID> result = idempotencyService.get("new-key");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return stored transaction ID when same key is used again")
    void should_ReturnStoredId_When_SameKeyUsedTwice() {
        UUID existingTxId = UUID.randomUUID();
        String key = "duplicate-key";

        // First request: key not yet stored
        when(valueOperations.get("idempotency:" + key)).thenReturn(null);
        assertThat(idempotencyService.get(key)).isEmpty();

        // Store the result of the first request
        idempotencyService.store(key, existingTxId);

        // Second request: same key → return previous result
        when(valueOperations.get("idempotency:" + key)).thenReturn(existingTxId.toString());
        assertThat(idempotencyService.get(key)).contains(existingTxId);
    }

    @Test
    @DisplayName("should return empty for a different key that has not been processed")
    void should_ReturnEmpty_When_DifferentKeyRequested() {
        UUID existingTxId = UUID.randomUUID();

        when(valueOperations.get("idempotency:key-processed")).thenReturn(existingTxId.toString());
        when(valueOperations.get("idempotency:key-new")).thenReturn(null);

        assertThat(idempotencyService.get("key-processed")).contains(existingTxId);
        assertThat(idempotencyService.get("key-new")).isEmpty();
    }

    @Test
    @DisplayName("should store transaction ID with correct key prefix and 24h TTL")
    void should_StoreIdWithCorrectKeyPrefixAndTTL() {
        UUID txId = UUID.randomUUID();
        String key = "my-idempotency-key";

        idempotencyService.store(key, txId);

        verify(valueOperations).set(
            eq("idempotency:" + key),
            eq(txId.toString()),
            eq(24L),
            eq(TimeUnit.HOURS)
        );
    }
}
