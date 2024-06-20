package com.gateway.filter;

import com.gateway.ratelimit.TokenBucketRateLimiter;
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
 * Global filter that applies rate limiting to all incoming requests.
 * Uses client IP address as the rate limit key.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final TokenBucketRateLimiter rateLimiter;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = getClientIp(exchange);

        return rateLimiter.tryConsume(clientIp)
                .flatMap(allowed -> {
                    if (allowed) {
                        // Request allowed, proceed with filter chain
                        return chain.filter(exchange);
                    } else {
                        // Rate limit exceeded
                        log.warn("Rate limit exceeded for IP: {}", clientIp);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        exchange.getResponse().getHeaders().add("X-RateLimit-Retry-After", "60");
                        return exchange.getResponse().setComplete();
                    }
                });
    }

    private String getClientIp(ServerWebExchange exchange) {
        // Check X-Forwarded-For header first (for proxied requests)
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }

        // Fall back to remote address
        return exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    @Override
    public int getOrder() {
        // Execute early in the filter chain
        return -100;
    }
}
