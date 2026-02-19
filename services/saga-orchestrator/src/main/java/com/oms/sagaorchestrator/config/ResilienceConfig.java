package com.oms.sagaorchestrator.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j Circuit Breaker configuration for saga orchestrator.
 *
 * Protects Kafka command dispatches to downstream services (payment,
 * inventory).
 * If a downstream Kafka topic becomes unavailable or the service repeatedly
 * fails,
 * the circuit opens and the saga falls back gracefully — preventing cascading
 * failures.
 *
 * States:
 * CLOSED → normal, requests pass through
 * OPEN → short-circuits immediately, fallback fires
 * HALF_OPEN → test probe: allows limited calls to check recovery
 */
@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                // Look at a rolling window of 10 calls
                .slidingWindowSize(10)
                // Open circuit if ≥50% of calls fail
                .failureRateThreshold(50)
                // Stay open 60 seconds before probing
                .waitDurationInOpenState(Duration.ofSeconds(60))
                // Allow 3 test calls in HALF_OPEN state
                .permittedNumberOfCallsInHalfOpenState(3)
                // Exceptions that count as failures
                .recordExceptions(Exception.class)
                .build();

        return CircuitBreakerRegistry.of(config);
    }
}
