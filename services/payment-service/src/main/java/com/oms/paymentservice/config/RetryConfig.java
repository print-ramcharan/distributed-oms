package com.oms.paymentservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Enables Spring Retry across the payment-service application.
 *
 * With @EnableRetry, methods annotated with @Retryable will automatically
 * retry on transient failures (e.g., DB connection hiccup, brief timeout).
 * The @Recover method fires after all retries are exhausted — we use it
 * to publish the failing message to the DLQ instead of crashing.
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // Retry behaviour is declared via @Retryable on individual service methods.
    // Backoff: 1s → 2s → 4s (exponential, max 3 attempts).
}
