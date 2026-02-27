package com.oms.orderservice.application.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.orderservice.domain.outbox.OutboxEvent;
import com.oms.orderservice.domain.outbox.OutboxRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class OrderOutboxRelay {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRIES = 3;

    public OrderOutboxRelay(
            OutboxRepository outboxRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @Scheduled(fixedDelay = 500)
    public void publishPendingEvents() {

        List<OutboxEvent> events = outboxRepository.fetchPendingEvents(50);

        for (OutboxEvent event : events) {
            try {
                Object payload = objectMapper.readTree(event.getPayload());

                kafkaTemplate.send(
                        resolveTopic(event),
                        event.getAggregateId().toString(),
                        payload
                );

                event.markSent();

            } catch (Exception ex) {

                if (event.getRetryCount() >= MAX_RETRIES) {
                    log.error("Outbox event {} permanently failed", event.getId(), ex);
                    event.markFailed();
                } else {
                    event.incrementRetry();
                    event.scheduleNextRetry();
                }
            }
        }
    }

    private String resolveTopic(OutboxEvent event) {
        return switch (event.getEventType()) {
            case "OrderCreatedEvent" -> "order.event.created";
            case "OrderProgressUpdatedEvent" -> "order.event.progress-updated";
            default -> throw new IllegalStateException(
                    "Unknown event type: " + event.getEventType()
            );
        };
    }
}
