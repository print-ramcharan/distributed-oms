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

        
        if (fulfillmentTaskRepository.findByOrderId(orderId).isPresent()) {
            log.warn("⚠️  FulfillmentTask already exists for orderId={} — skipping", orderId);
            return;
        }

        
        FulfillmentTask task = FulfillmentTask.create(orderId);
        fulfillmentTaskRepository.save(task);
        log.info("✅ FulfillmentTask created | taskId={} orderId={}", task.getId(), orderId);

        
        FulfillmentInitiatedEvent initiatedEvent = new FulfillmentInitiatedEvent(
                orderId,
                task.getId().toString(),
                task.getCreatedAt());

        kafkaTemplate.send("fulfillment.initiated", orderId, initiatedEvent);
        log.info("📤 FulfillmentInitiatedEvent published | taskId={} orderId={}", task.getId(), orderId);

        
        task.dispatch();
        fulfillmentTaskRepository.save(task);
    }
}
