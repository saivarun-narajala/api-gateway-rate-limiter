package com.gateway.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Token Bucket implementation for distributed rate limiting using Redis.
 * 
 * The token bucket algorithm allows burst traffic while enforcing average rate
 * limits.
 * Each bucket has a capacity and refill rate. Requests consume tokens, and if
 * no tokens
 * are available, the request is rate limited.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBucketRateLimiter {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    // Configuration
    private static final long BUCKET_CAPACITY = 100; // Max tokens
    private static final long REFILL_RATE = 10; // Tokens per second
    private static final Duration TTL = Duration.ofMinutes(5);

    /**
     * Attempt to consume a token from the bucket.
     * 
     * @param key Unique identifier for the rate limit (e.g., user ID, IP address)
     * @return true if request is allowed, false if rate limited
     */
    public Mono<Boolean> tryConsume(String key) {
        String bucketKey = "rate_limit:bucket:" + key;
        String timestampKey = "rate_limit:timestamp:" + key;

        return getCurrentTokens(bucketKey, timestampKey)
                .flatMap(tokens -> {
                    if (tokens >= 1) {
                        // Consume one token
                        return redisTemplate.opsForValue()
                                .decrement(bucketKey)
                                .map(remaining -> {
                                    log.debug("Request allowed for key: {} (tokens remaining: {})", key, remaining);
                                    return true;
                                });
                    } else {
                        log.warn("Rate limit exceeded for key: {}", key);
                        return Mono.just(false);
                    }
                });
    }

    /**
     * Get current token count, refilling based on elapsed time.
     */
    private Mono<Long> getCurrentTokens(String bucketKey, String timestampKey) {
        long now = Instant.now().toEpochMilli();

        return redisTemplate.opsForValue().get(bucketKey)
                .zipWith(redisTemplate.opsForValue().get(timestampKey))
                .flatMap(tuple -> {
                    long currentTokens = Long.parseLong(tuple.getT1());
                    long lastRefill = Long.parseLong(tuple.getT2());

                    // Calculate tokens to add based on elapsed time
                    long elapsedMs = now - lastRefill;
                    long tokensToAdd = (elapsedMs * REFILL_RATE) / 1000;

                    if (tokensToAdd > 0) {
                        long newTokens = Math.min(BUCKET_CAPACITY, currentTokens + tokensToAdd);

                        return redisTemplate.opsForValue().set(bucketKey, String.valueOf(newTokens), TTL)
                                .then(redisTemplate.opsForValue().set(timestampKey, String.valueOf(now), TTL))
                                .thenReturn(newTokens);
                    }

                    return Mono.just(currentTokens);
                })
                .switchIfEmpty(initializeBucket(bucketKey, timestampKey));
    }

    /**
     * Initialize a new bucket with full capacity.
     */
    private Mono<Long> initializeBucket(String bucketKey, String timestampKey) {
        long now = Instant.now().toEpochMilli();

        return redisTemplate.opsForValue().set(bucketKey, String.valueOf(BUCKET_CAPACITY), TTL)
                .then(redisTemplate.opsForValue().set(timestampKey, String.valueOf(now), TTL))
                .thenReturn(BUCKET_CAPACITY);
    }

    /**
     * Get remaining tokens for a key (for monitoring/debugging).
     */
    public Mono<Long> getRemainingTokens(String key) {
        String bucketKey = "rate_limit:bucket:" + key;
        String timestampKey = "rate_limit:timestamp:" + key;
        return getCurrentTokens(bucketKey, timestampKey);
    }
}
