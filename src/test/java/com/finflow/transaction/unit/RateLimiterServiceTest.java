package com.finflow.transaction.unit;

import com.finflow.shared.util.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("should allow request when within rate limit")
    void should_AllowRequest_When_WithinRateLimit() {
        UUID accountId = UUID.randomUUID();
        when(valueOperations.increment(anyString())).thenReturn(5L);

        assertThat(rateLimiterService.isAllowed(accountId)).isTrue();
    }

    @Test
    @DisplayName("should set expiry only on the first request to start the fixed window")
    void should_SetExpiry_OnlyOnFirstRequest() {
        UUID accountId = UUID.randomUUID();
        String expectedKey = "rate_limit:transfer:" + accountId;

        when(valueOperations.increment(anyString())).thenReturn(1L);

        rateLimiterService.isAllowed(accountId);

        verify(redisTemplate).expire(eq(expectedKey), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("should allow first ten requests and block the eleventh")
    void should_AllowFirstTenRequests_And_BlockEleventh_When_RateLimitReached() {
        UUID accountId = UUID.randomUUID();
        when(valueOperations.increment(anyString()))
            .thenReturn(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L);
        lenient().when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        for (int i = 0; i < 10; i++) {
            assertThat(rateLimiterService.isAllowed(accountId))
                .as("Request %d should be allowed", i + 1)
                .isTrue();
        }

        // 11th request exceeds the limit
        assertThat(rateLimiterService.isAllowed(accountId))
            .as("11th request should be blocked")
            .isFalse();
    }
}
