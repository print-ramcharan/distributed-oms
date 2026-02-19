package com.oms.paymentservice.service;

import com.oms.eventcontracts.events.PaymentCompletedEvent;
import com.oms.eventcontracts.events.PaymentRefundedEvent;
import com.oms.paymentservice.domain.Payment;
import com.oms.paymentservice.domain.PaymentStatus;
import com.oms.paymentservice.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


@Service
@Slf4j
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Payment createPayment(UUID orderId, BigDecimal amount, String idempotencyKey) {
        if (idempotencyKey != null && paymentRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return paymentRepository.findByIdempotencyKey(idempotencyKey).get();
        }

        return paymentRepository.findByOrderId(orderId)
                .orElseGet(() -> {
                    String transactionId = UUID.randomUUID().toString();
                    Payment payment = new Payment(orderId, amount, transactionId, idempotencyKey, "CREDIT_CARD");
                    return paymentRepository.save(payment);
                });
    }

    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return idempotencyKey != null && paymentRepository.findByIdempotencyKey(idempotencyKey).isPresent();
    }

    
    @Transactional
    @Retryable(retryFor = {
            Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public Payment processPayment(UUID orderId) {
        log.info("💳 Attempting to process payment for orderId={}", orderId);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for orderId: " + orderId));

        payment.markCompleted();
        Payment savedPayment = paymentRepository.save(payment);

        PaymentCompletedEvent event = new PaymentCompletedEvent(
                savedPayment.getOrderId(),
                savedPayment.getId(),
                savedPayment.getAmount(),
                Instant.now());
        kafkaTemplate.send("payment.completed", orderId.toString(), event);

        log.info("✅ Published PaymentCompletedEvent | orderId={} | paymentId={}",
                orderId, savedPayment.getId());

        return savedPayment;
    }

    
    @Recover
    @Transactional
    public Payment recoverProcessPayment(Exception ex, UUID orderId) {
        log.error("🚨 All retries exhausted for processPayment | orderId={} | error={}",
                orderId, ex.getMessage());

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment != null) {
            payment.markFailed();
            paymentRepository.save(payment);
            log.warn("⚠️ Payment marked FAILED after retry exhaustion | orderId={}", orderId);
        }

        
        return payment;
    }

    @Transactional
    public Payment refundPayment(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for orderId: " + orderId));

        payment.markRefunded();
        Payment savedPayment = paymentRepository.save(payment);

        PaymentRefundedEvent event = new PaymentRefundedEvent(
                savedPayment.getOrderId(),
                savedPayment.getId(),
                savedPayment.getAmount(),
                "Refunded due to saga failure",
                Instant.now());
        kafkaTemplate.send("payment.refunded", orderId.toString(), event);
        log.info("💰 Published PaymentRefundedEvent | orderId={} | paymentId={}",
                orderId, savedPayment.getId());

        return savedPayment;
    }

    @Transactional
    public Payment markPaymentFailed(UUID orderId, String reason) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for orderId: " + orderId));
        payment.markFailed();
        return paymentRepository.save(payment);
    }

    

    @Transactional
    public Payment createPendingPayment(UUID orderId, BigDecimal amount) {
        return createPayment(orderId, amount, null);
    }

    @Transactional
    public Payment completePayment(UUID orderId) {
        return processPayment(orderId);
    }
}
