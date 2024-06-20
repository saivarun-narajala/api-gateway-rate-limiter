package com.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class Resilience4jConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                // Open circuit after 50% failure rate
                .failureRateThreshold(50)
                // Minimum number of calls before calculating failure rate
                .minimumNumberOfCalls(5)
                // Wait 30 seconds before trying again (half-open state)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                // Allow 3 calls in half-open state to test recovery
                .permittedNumberOfCallsInHalfOpenState(3)
                // Size of sliding window for calculating failure rate
                .slidingWindowSize(10)
                .build();

        return CircuitBreakerRegistry.of(config);
    }
}
