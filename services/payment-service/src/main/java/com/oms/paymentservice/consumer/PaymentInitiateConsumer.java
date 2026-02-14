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

            // 1. Strict Idempotency Check (Redis)
            if (Boolean.TRUE.equals(redisTemplate.hasKey(idempotencyKey))) {
                log.info("⏭️ Duplicate Payment Request detected (Redis) | orderId={}", orderId);
                return;
            }

            // 2. Strict Idempotency Check (Database)
            // Even if Redis key expired, check if payment exists with this orderId/key
            if (paymentService.existsByIdempotencyKey(idempotencyKey)) {
                log.info("⏭️ Duplicate Payment Request detected (DB) | orderId={}", orderId);
                return;
            }

            log.info("💳 InitiatePaymentCommand received | orderId={} | amount={}",
                    command.getOrderId(), command.getAmount());

            // 3. Process Payment
            // Pass the idempotency key to be saved in DB
            paymentService.createPayment(command.getOrderId(), command.getAmount(), idempotencyKey);
            log.info("✓ Payment created with PENDING status | orderId={}", command.getOrderId());

            // Simulate payment gateway success and mark completed
            paymentService.completePayment(command.getOrderId());

            // 4. Mark as Processed in Redis (24h TTL)
            redisTemplate.opsForValue().set(idempotencyKey, "COMPLETED", java.time.Duration.ofHours(24));

            log.info("✅ Payment completed successfully | orderId={}", command.getOrderId());

        } catch (Exception e) {
            log.error("❌ Payment processing failed | orderId={} | error={}",
                    command.getOrderId(), e.getMessage(), e);
            throw e; // Re-throw to trigger Kafka retry/DLQ
        }
    }
}
