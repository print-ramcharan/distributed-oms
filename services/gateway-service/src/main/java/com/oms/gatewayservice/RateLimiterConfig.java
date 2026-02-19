package com.oms.gatewayservice;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Rate limiter configuration for the API Gateway.
 *
 * - ipKeyResolver: rate-limits per client IP address
 * - userKeyResolver: rate-limits per X-User-Id header (falls back to IP)
 *
 * The active resolver is wired via application.yml's key-resolver SpEL ref.
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Rate-limit key: client IP from X-Forwarded-For or remote address.
     * Used for unauthenticated endpoints (e.g. POST /api/orders from anonymous
     * clients).
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : "unknown";
            }
            return Mono.just(ip);
        };
    }

    /**
     * Rate-limit key: user identity from X-User-Id header.
     * Falls back to IP if header is absent (e.g. pre-auth traffic).
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + ip);
        };
    }
}
