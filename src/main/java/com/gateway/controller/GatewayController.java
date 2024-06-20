package com.gateway.controller;

import com.gateway.ratelimit.TokenBucketRateLimiter;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/gateway")
@RequiredArgsConstructor
public class GatewayController {

    private final TokenBucketRateLimiter rateLimiter;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @GetMapping("/health")
    public Mono<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "api-gateway");
        return Mono.just(health);
    }

    @GetMapping("/rate-limit/status")
    public Mono<Map<String, Object>> getRateLimitStatus(@RequestParam String key) {
        return rateLimiter.getRemainingTokens(key)
                .map(tokens -> {
                    Map<String, Object> status = new HashMap<>();
                    status.put("key", key);
                    status.put("remainingTokens", tokens);
                    status.put("capacity", 100);
                    status.put("refillRate", "10 tokens/second");
                    return status;
                });
    }

    @GetMapping("/circuit-breaker/status")
    public Mono<Map<String, Object>> getCircuitBreakerStatus(@RequestParam String service) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(service);

        Map<String, Object> status = new HashMap<>();
        status.put("service", service);
        status.put("state", circuitBreaker.getState().toString());
        status.put("failureRate", circuitBreaker.getMetrics().getFailureRate());
        status.put("numberOfCalls", circuitBreaker.getMetrics().getNumberOfBufferedCalls());
        status.put("numberOfFailedCalls", circuitBreaker.getMetrics().getNumberOfFailedCalls());

        return Mono.just(status);
    }
}
