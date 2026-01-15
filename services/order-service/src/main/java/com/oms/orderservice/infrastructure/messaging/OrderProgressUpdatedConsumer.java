package com.oms.orderservice.infrastructure.messaging;

import com.oms.eventcontracts.events.OrderProgressUpdatedEvent;
import com.oms.orderservice.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderProgressUpdatedConsumer {

    private final OrderRepository orderRepository;

    @KafkaListener(
            topics = "order.progress.updated",
            groupId = "order-service"
    )
    @Transactional
    public void handle(OrderProgressUpdatedEvent event) {
        orderRepository.findById(event.getOrderId())
                .ifPresent(order -> {
                    order.advanceProgress(event.getProgress());
                });
    }
}

