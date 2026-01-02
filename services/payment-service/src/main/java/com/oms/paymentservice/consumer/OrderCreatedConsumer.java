package com.oms.paymentservice.consumer;

import com.oms.events.OrderCreatedEvent;
import com.oms.paymentservice.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedConsumer {

    private final PaymentService paymentService;

    OrderCreatedConsumer(PaymentService paymentService){
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "order.created", groupId = "payment-service", containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderCreated(OrderCreatedEvent event) {
        System.out.println("Consumer OrderCreatedEvent" + event);
        paymentService.createPendingPayment(event.getOrderId(), event.getAmount());

        System.out.println("Payment created for orderId: " + event.getOrderId());
    }
}
