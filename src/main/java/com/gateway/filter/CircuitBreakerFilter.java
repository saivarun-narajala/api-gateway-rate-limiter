package com.gateway.filter;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Circuit breaker filter to prevent cascading failures.
 * 
 * When downstream services fail repeatedly, the circuit breaker opens and
 * immediately returns errors without calling the failing service, giving it
 * time to recover.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CircuitBreakerFilter implements GlobalFilter, Ordered {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String serviceName = extractServiceName(exchange);

        return chain.filter(exchange)
                .transformDeferred(CircuitBreakerOperator.of(
                        circuitBreakerRegistry.circuitBreaker(serviceName)))
                .onErrorResume(throwable -> {
                    log.error("Circuit breaker triggered for service: {}", serviceName, throwable);
                    exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                    exchange.getResponse().getHeaders().add("X-Circuit-Breaker", "OPEN");
                    return exchange.getResponse().setComplete();
                });
    }

    private String extractServiceName(ServerWebExchange exchange) {
        // Extract service name from path (e.g., /api/users -> users-service)
        String path = exchange.getRequest().getPath().value();
        String[] segments = path.split("/");

        if (segments.length > 2) {
            return segments[2] + "-service";
        }

        return "default-service";
    }

    @Override
    public int getOrder() {
        // Execute after rate limiting but before routing
        return -50;
    }
}
