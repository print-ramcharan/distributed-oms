package com.oms.sagaorchestrator.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;


@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                
                .slidingWindowSize(10)
                
                .failureRateThreshold(50)
                
                .waitDurationInOpenState(Duration.ofSeconds(60))
                
                .permittedNumberOfCallsInHalfOpenState(3)
                
                .recordExceptions(Exception.class)
                .build();

        return CircuitBreakerRegistry.of(config);
    }
}
