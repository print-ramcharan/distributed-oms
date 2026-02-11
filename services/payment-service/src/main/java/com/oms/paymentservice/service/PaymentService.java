package com.oms.paymentservice.service;

import com.oms.paymentservice.domain.Payment;
import com.oms.paymentservice.domain.PaymentStatus;
import com.oms.paymentservice.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Transactional
    public Payment createPayment(UUID orderId, BigDecimal amount) {
        // Idempotency: check if payment already exists
        return paymentRepository.findByOrderId(orderId)
                .orElseGet(() -> {
                    Payment payment = new Payment(orderId, amount);
                    return paymentRepository.save(payment);
                });
    }

    @Transactional
    public Payment processPayment(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for orderId: " + orderId));

        payment.markCompleted();
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment refundPayment(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for orderId: " + orderId));

        payment.markRefunded();
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment markPaymentFailed(UUID orderId, String reason) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for orderId: " + orderId));

        payment.markFailed(reason);
        return paymentRepository.save(payment);
    }

    // ========== Backward-compatible methods for existing consumers ==========

    @Transactional
    public Payment createPendingPayment(UUID orderId, BigDecimal amount) {
        return createPayment(orderId, amount);
    }

    @Transactional
    public Payment completePayment(UUID orderId) {
        return processPayment(orderId);
    }
}
