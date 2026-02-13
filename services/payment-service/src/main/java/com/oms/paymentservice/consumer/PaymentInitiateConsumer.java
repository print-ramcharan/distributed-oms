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

    PaymentInitiateConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "payment.initiate", groupId = "payment-service", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handleOrderCreated(InitiatePaymentCommand command) {
        try {
            log.info("💳 InitiatePaymentCommand received | orderId={} | amount={}",
                    command.getOrderId(), command.getAmount());

            // Create pending payment
            paymentService.createPendingPayment(command.getOrderId(), command.getAmount());
            log.info("✓ Payment created with PENDING status | orderId={}", command.getOrderId());

            // Simulate payment gateway success and mark completed
            paymentService.completePayment(command.getOrderId());
            log.info("✅ Payment completed successfully | orderId={}", command.getOrderId());

        } catch (Exception e) {
            log.error("❌ Payment processing failed | orderId={} | error={}",
                    command.getOrderId(), e.getMessage(), e);
            throw e; // Re-throw to trigger Kafka retry/DLQ
        }
    }
}
