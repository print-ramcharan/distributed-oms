package com.oms.orderservice.infrastructure.chaos;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ChaosAspect {

    @Before("execution(* com.oms.orderservice.application.OrderCommandService.createOrder(..))")
    public void injectChaos() {
        if (ChaosController.killOrderService) {
            throw new RuntimeException("Chaos Engineering: Service Killed via /chaos/kill-service");
        }
        if (ChaosController.latencyEnabled) {
            try {
                System.out.println("Chaos Engineering: Injecting 5s latency...");
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
