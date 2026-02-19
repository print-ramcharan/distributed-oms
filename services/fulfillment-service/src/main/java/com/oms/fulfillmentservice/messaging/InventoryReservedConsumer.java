package com.oms.fulfillmentservice.messaging;

import com.oms.eventcontracts.events.InventoryReservedEvent;
import com.oms.fulfillmentservice.domain.FulfillmentTask;
import com.oms.fulfillmentservice.domain.FulfillmentTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listens on inventory.reserved — the event published by inventory-service
 * after stock is successfully reserved for an order.
 *
 * For each event:
 * 1. Creates a FulfillmentTask (INITIATED)
 * 2. Saves to DB
 * 3. Publishes FulfillmentInitiatedEvent to fulfillment.initiated topic
 * 4. Marks task as DISPATCHED
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryReservedConsumer {

    private final FulfillmentTaskRepository fulfillmentTaskRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "inventory.reserved", groupId = "fulfillment-service", containerFactory = "fulfillmentKafkaListenerContainerFactory")
    @Transactional
    public void handle(InventoryReservedEvent event) {
        String orderId = event.getOrderId();

        log.info("📦 InventoryReservedEvent received | orderId={}", orderId);

        // Idempotency guard — skip if task already exists
        if (fulfillmentTaskRepository.findByOrderId(orderId).isPresent()) {
            log.warn("⚠️  FulfillmentTask already exists for orderId={} — skipping", orderId);
            return;
        }

        // 1. Create task
        FulfillmentTask task = FulfillmentTask.create(orderId);
        fulfillmentTaskRepository.save(task);
        log.info("✅ FulfillmentTask created | taskId={} orderId={}", task.getId(), orderId);

        // 2. Publish fulfillment.initiated
        FulfillmentInitiatedEvent initiatedEvent = new FulfillmentInitiatedEvent(
                orderId,
                task.getId().toString(),
                task.getCreatedAt());

        kafkaTemplate.send("fulfillment.initiated", orderId, initiatedEvent);
        log.info("📤 FulfillmentInitiatedEvent published | taskId={} orderId={}", task.getId(), orderId);

        // 3. Mark as dispatched
        task.dispatch();
        fulfillmentTaskRepository.save(task);
    }
}
