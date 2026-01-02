package com.oms.paymentservice.consumer;

import com.oms.paymentservice.event.OrderCreatedEvent;
import com.oms.paymentservice.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedConsumer {

    private final PaymentService paymentService;

    OrderCreatedConsumer(PaymentService paymentService){
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "order-created", groupId = "payment-service")
    public void handleOrderCreated(OrderCreatedEvent event){
        paymentService.createPendingPayment(event.getOrderId(), event.getAmount());
        System.out.println("Payment created for orderId:" + event.getOrderId());
    }
}
