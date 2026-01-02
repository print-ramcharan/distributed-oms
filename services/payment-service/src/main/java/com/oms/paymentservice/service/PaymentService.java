package com.oms.paymentservice.service;

import com.oms.paymentservice.entity.Payment;
import com.oms.paymentservice.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository){
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Payment createPendingPayment(UUID orderId, BigDecimal amount){
        return paymentRepository.findByOrderId(orderId)
                                .orElseGet(() -> {
                                    Payment payment = Payment.builder().id(UUID.randomUUID())
                                                                       .orderId(orderId)
                                                                       .amount(amount)
                                                                       .currency("INR")
                                                                       .status("PENDING")
                                                                       .createdAt(Instant.now())
                                                                       .updatedAt(Instant.now()).build();
                                    return paymentRepository.save(payment);
                                });
    }
}
