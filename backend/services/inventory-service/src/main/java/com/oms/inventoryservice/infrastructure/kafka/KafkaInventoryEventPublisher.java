package com.oms.inventoryservice.infrastructure.kafka;

import com.oms.inventoryservice.domain.event.InventoryEventPublisher;
import com.oms.eventcontracts.events.InventoryReservedEvent;
import com.oms.eventcontracts.events.InventoryUnavailableEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaInventoryEventPublisher implements InventoryEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.inventory-reserved}")
    private String inventoryReservedTopic;

    @Value("${kafka.topics.inventory-unavailable}")
    private String inventoryUnavailableTopic;

    @Override
    public void publishInventoryReserved(InventoryReservedEvent event) {
        kafkaTemplate.send(
                inventoryReservedTopic,
                event.getOrderId(),
                event).whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish InventoryReservedEvent", ex);
                    }
                });
    }

    @Override
    public void publishInventoryUnavailable(InventoryUnavailableEvent event) {
        try {
            log.info("Attempting to publish InventoryUnavailableEvent: {}", event);
            var result = kafkaTemplate.send(
                    inventoryUnavailableTopic,
                    event.getOrderId(),
                    event).get(); 
            log.info("Successfully published InventoryUnavailableEvent: {}", result.getRecordMetadata());
        } catch (Exception e) {
            log.error("FATAL ERROR publishing InventoryUnavailableEvent", e);
            throw new RuntimeException(e);
        }
    }
}
