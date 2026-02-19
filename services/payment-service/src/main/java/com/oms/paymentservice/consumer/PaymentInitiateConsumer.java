package com.oms.paymentservice.consumer;

import com.oms.eventcontracts.commands.InitiatePaymentCommand;
import com.oms.paymentservice.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class PaymentInitiateConsumer {

    private final PaymentService paymentService;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    PaymentInitiateConsumer(PaymentService paymentService,
            org.springframework.data.redis.core.StringRedisTemplate redisTemplate) {
        this.paymentService = paymentService;
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(topics = "payment.initiate", groupId = "payment-service", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handleOrderCreated(InitiatePaymentCommand command) {
        try {
            String orderId = command.getOrderId().toString();
            String idempotencyKey = "payment:idempotency:" + orderId;

            
            if (Boolean.TRUE.equals(redisTemplate.hasKey(idempotencyKey))) {
                log.info("⏭️ Duplicate Payment Request detected (Redis) | orderId={}", orderId);
                return;
            }

            
            
            if (paymentService.existsByIdempotencyKey(idempotencyKey)) {
                log.info("⏭️ Duplicate Payment Request detected (DB) | orderId={}", orderId);
                return;
            }

            log.info("💳 InitiatePaymentCommand received | orderId={} | amount={}",
                    command.getOrderId(), command.getAmount());

            
            
            paymentService.createPayment(command.getOrderId(), command.getAmount(), idempotencyKey);
            log.info("✓ Payment created with PENDING status | orderId={}", command.getOrderId());

            
            paymentService.completePayment(command.getOrderId());

            
            redisTemplate.opsForValue().set(idempotencyKey, "COMPLETED", java.time.Duration.ofHours(24));

            log.info("✅ Payment completed successfully | orderId={}", command.getOrderId());

        } catch (Exception e) {
            log.error("❌ Payment processing failed | orderId={} | error={}",
                    command.getOrderId(), e.getMessage(), e);
            throw e; 
        }
    }
}
