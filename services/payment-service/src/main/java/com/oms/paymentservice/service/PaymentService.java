package com.oms.paymentservice.service;

import com.oms.eventcontracts.events.PaymentCompletedEvent;
import com.oms.eventcontracts.events.PaymentRefundedEvent;
import com.oms.paymentservice.domain.Payment;
import com.oms.paymentservice.domain.PaymentStatus;
import com.oms.paymentservice.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
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
        // Idempotency: check if payment already exists by orderId (business key) or
        // idempotencyKey
        if (idempotencyKey != null && paymentRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return paymentRepository.findByIdempotencyKey(idempotencyKey).get();
        }

        return paymentRepository.findByOrderId(orderId)
                .orElseGet(() -> {
                    // Generate a transaction ID for the new payment
                    String transactionId = UUID.randomUUID().toString();
                    // Default payment method for now, can be passed in later
                    String paymentMethod = "CREDIT_CARD";

                    Payment payment = new Payment(orderId, amount, transactionId, idempotencyKey, paymentMethod);
                    return paymentRepository.save(payment);
                });
    }

    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return idempotencyKey != null && paymentRepository.findByIdempotencyKey(idempotencyKey).isPresent();
    }

    @Transactional
    public Payment processPayment(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for orderId: " + orderId));

        payment.markCompleted();
        Payment savedPayment = paymentRepository.save(payment);

        // Publish PaymentCompletedEvent to Kafka
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

    @Transactional
    public Payment refundPayment(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for orderId: " + orderId));

        payment.markRefunded();
        Payment savedPayment = paymentRepository.save(payment);

        // Publish PaymentRefundedEvent to Kafka
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

    // ========== Backward-compatible methods for existing consumers ==========

    @Transactional
    public Payment createPendingPayment(UUID orderId, BigDecimal amount) {
        return createPayment(orderId, amount, null); // Backward compatibility: no idempotency key
    }

    @Transactional
    public Payment completePayment(UUID orderId) {
        return processPayment(orderId);
    }
}
