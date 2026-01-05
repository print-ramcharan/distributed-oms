package com.oms.orderservice.application.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.eventcontracts.events.OrderCreatedEvent;
import com.oms.orderservice.domain.event.OrderEventPublisher;
import com.oms.orderservice.domain.outbox.OutboxEvent;
import com.oms.orderservice.domain.outbox.OutboxRepository;
import com.oms.orderservice.infrastructure.messaging.KafkaOrderEventPublisher;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class OrderOutboxRelay {

    private final OutboxRepository outboxRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final KafkaOrderEventPublisher kafkaOrderEventPublisher;
    private final ObjectMapper objectMapper;
    private static final int MAX_RETRIES = 3;

    public OrderOutboxRelay(
            OutboxRepository outboxRepository,
            OrderEventPublisher orderEventPublisher,
            KafkaOrderEventPublisher kafkaOrderEventPublisher,
            ObjectMapper objectMapper
    ) {
        this.outboxRepository = outboxRepository;
        this.orderEventPublisher = orderEventPublisher;
        this.kafkaOrderEventPublisher = kafkaOrderEventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @Scheduled(fixedDelay = 500)
    public void publishPendingEvents() {

        List<OutboxEvent> events = outboxRepository.fetchPendingEvents(50);

        for (OutboxEvent event : events) {
            try {
                OrderCreatedEvent domainEvent = deserialize(event);
                orderEventPublisher.publish(domainEvent);
                event.markSent();

            } catch (Exception ex) {

                PublishFailureType failureType =
                        PublishFailureClassifier.classify(ex);

                switch (failureType) {
                    case PERMANENT -> handlePermanentFailure(event, ex);
                    case TRANSIENT -> {
                        if (event.getRetryCount() >= MAX_RETRIES) {
                            log.error("Max retries exceeded for event {}, moving to DLQ", event.getId());
                            handlePermanentFailure(event, ex);
                            break;
                        }

                        event.incrementRetry();
                        event.scheduleNextRetry();

                        log.warn("Transient failure, retry #{} for event {}",
                                event.getRetryCount(), event.getId());
                    }


                }
            }
        }
    }

    private void handlePermanentFailure(OutboxEvent event, Exception ex) {
        try {
            OrderCreatedEvent domainEvent = deserialize(event);
            kafkaOrderEventPublisher.publishToDlq(domainEvent, ex);
            event.markFailed();
        } catch (Exception dlqEx) {
            log.error("DLQ publish failed, keeping event NEW", dlqEx);
        }
    }

    private OrderCreatedEvent deserialize(OutboxEvent event) {
        try {
            return objectMapper.readValue(
                    event.getPayload(),
                    OrderCreatedEvent.class
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to deserialize outbox payload",
                    e
            );
        }
    }
}
