package com.oms.paymentservice.service;

import com.oms.eventcontracts.events.PaymentCompletedEvent;
import com.oms.eventcontracts.events.PaymentFailedEvent;
import com.oms.paymentservice.entity.Payment;
import com.oms.paymentservice.entity.PaymentStatus;
import com.oms.paymentservice.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentService(PaymentRepository paymentRepository, KafkaTemplate<String, Object> kafkaTemplate){
        this.paymentRepository = paymentRepository;
        this.kafkaTemplate = kafkaTemplate;
    }


    @Transactional
    public Payment createPendingPayment(UUID orderId, BigDecimal amount){
        return paymentRepository.findByOrderId(orderId)
                                .orElseGet(() -> {
                                    Payment payment = Payment.builder().id(UUID.randomUUID())
                                                                       .orderId(orderId)
                                                                       .amount(amount)
                                                                       .currency("INR")
                                                                       .status(PaymentStatus.PENDING)
                                                                       .createdAt(Instant.now())
                                                                       .updatedAt(Instant.now()).build();
                                    return paymentRepository.save(payment);
                                });
    }

    @Transactional
    public void completePayment(UUID orderId){
        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow();

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setUpdatedAt(Instant.now());

        paymentRepository.save(payment);
        Instant instant = Instant.now();
        kafkaTemplate.send("payment.completed", new PaymentCompletedEvent(payment.getOrderId(), payment.getId(), payment.getAmount(), instant));

    }

    @Transactional
    public void failPayment(UUID orderId, String reason){
        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow();

        payment.setStatus(PaymentStatus.FAILED);
        payment.setUpdatedAt(Instant.now());

        paymentRepository.save(payment);
        Instant instant = Instant.now();

        kafkaTemplate.send("payment.failed", new PaymentFailedEvent(payment.getOrderId(), payment.getId(), reason, instant));
    }


}
