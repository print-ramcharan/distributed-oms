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
                event
        ).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish InventoryReservedEvent", ex);
            }
        });
    }

    @Override
    public void publishInventoryUnavailable(InventoryUnavailableEvent event) {
        kafkaTemplate.send(
                inventoryUnavailableTopic,
                event.getOrderId(),
                event
        ).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish InventoryUnavailableEvent", ex);
            }
        });
    }
}
