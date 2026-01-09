package com.oms.orderservice.infrastructure.messaging;

import com.oms.eventcontracts.events.OrderCompletedEvent;
import com.oms.eventcontracts.events.OrderFailedEvent;
import com.oms.orderservice.domain.model.Order;
import com.oms.orderservice.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaEventConsumer {

    private final OrderRepository orderRepository;

    @KafkaListener(
            topics = "order.completed",
            groupId = "order-service-v2", // 👈 CHANGED from 'order-service'
            containerFactory = "orderCompletedKafkaListenerContainerFactory"
    )
    public void onOrderCompleted(OrderCompletedEvent event, Acknowledgment ack) {
        try {
            log.info("🔥 Consumed OrderCompletedEvent for orderId={}", event.orderId());

            Order order = orderRepository.findById(event.orderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + event.orderId()));

            order.markCompleted();
            orderRepository.save(order);

            log.info("✅ Order status updated to COMPLETED in Database");
            ack.acknowledge();
        } catch (Exception e) {
            log.error("❌ Error processing OrderCompletedEvent", e);
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = "order.failed",
            groupId = "order-service-v2", // 👈 CHANGED here too
            containerFactory = "orderCompletedKafkaListenerContainerFactory"
    )
    public void onOrderFailed(OrderFailedEvent event, Acknowledgment ack) {
        try {
            log.info("💀 Consumed OrderFailedEvent for orderId={}", event.orderId());

            Order order = orderRepository.findById(event.orderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + event.orderId()));

            order.markFailed(event.reason());
            orderRepository.save(order);

            ack.acknowledge();
        } catch (Exception e) {
            log.error("❌ Error processing OrderFailedEvent", e);
            ack.acknowledge();
        }
    }
}