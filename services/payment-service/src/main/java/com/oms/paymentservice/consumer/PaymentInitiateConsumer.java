package com.oms.paymentservice.consumer;

import com.oms.eventcontracts.commands.InitiatePaymentCommand;
import com.oms.paymentservice.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentInitiateConsumer {

    private final PaymentService paymentService;

    PaymentInitiateConsumer(PaymentService paymentService){
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "payment.initiate", groupId = "payment-service", containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderCreated(InitiatePaymentCommand command) {
        System.out.println("Consumer OrderCreatedEvent" + command);
        paymentService.createPendingPayment(command.getOrderId(), command.getAmount());

        System.out.println("Payment created for orderId: " + command.getOrderId());
    }
}
