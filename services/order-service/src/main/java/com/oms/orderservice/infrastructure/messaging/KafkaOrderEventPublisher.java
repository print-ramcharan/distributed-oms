package com.oms.orderservice.infrastructure.messaging;

import com.oms.eventcontracts.events.OrderCreatedEvent;
import com.oms.orderservice.domain.event.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;


@Component
@Primary
@RequiredArgsConstructor
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "order.created";
    private static final String DLQ_TOPIC = "order.events.dlq";

    @Override
    public void publish(OrderCreatedEvent event) {
        try {
            kafkaTemplate
                    .send(TOPIC, event.getOrderId().toString(), event)
                    .get();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Kafka publish interrupted", ie);
        } catch (Exception ex) {
            throw new RuntimeException("Kafka publish failed", ex);
        }
    }


    public void publishToDlq(OrderCreatedEvent event, Exception ex) {
        DlqEnvelope envelope = new DlqEnvelope(
                "order-service",
                "order.created",
                event.getClass().getSimpleName(),
                event.getOrderId().toString(),
                event,
                ex.getMessage(),
                Instant.now()
        );

        kafkaTemplate.send("order.events.dlq", event.getOrderId().toString(), envelope);
    }

}