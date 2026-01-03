package com.oms.orderservice.infrastructure.messaging;

import com.oms.eventcontracts.events.OrderCreatedEvent;
import com.oms.eventcontracts.events.OrderCreatedEvent;
import com.oms.orderservice.domain.event.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    private static final String TOPIC = "order.created";


    @Override
    public void publish(OrderCreatedEvent event) {
        kafkaTemplate.send(TOPIC, event.getOrderId().toString(), event);
    }


}
