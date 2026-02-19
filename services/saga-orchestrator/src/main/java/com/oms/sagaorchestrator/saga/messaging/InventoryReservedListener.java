package com.oms.sagaorchestrator.saga.messaging;

import com.oms.eventcontracts.commands.AdvanceOrderProgressCommand;
import com.oms.eventcontracts.enums.OrderProgress;
import com.oms.eventcontracts.events.InventoryReservedEvent;
import com.oms.sagaorchestrator.saga.domain.OrderSaga;
import com.oms.sagaorchestrator.saga.domain.SagaState;
import com.oms.sagaorchestrator.saga.repository.OrderSagaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryReservedListener {

    private final OrderSagaRepository sagaRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(
            topics = "${kafka.topics.inventory-reserved}",
            groupId = "${kafka.consumer.group-id}",
            containerFactory = "inventoryReservedKafkaListenerContainerFactory"
    )
    @Transactional
    public void handle(InventoryReservedEvent event) {
        try {
            UUID orderId = UUID.fromString(event.getOrderId());
            log.info("Batch InventoryReservedEvent received | orderId={}", orderId);

            OrderSaga saga = sagaRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalStateException("Saga not found"));

            if (saga.getState() == SagaState.COMPLETED) {
                return;
            }
            saga.markInventoryReserved();

            
            saga.markCompleted();
            sagaRepository.save(saga);

            
            AdvanceOrderProgressCommand finalEvent =  new AdvanceOrderProgressCommand(
                    orderId,
                    OrderProgress.ORDER_COMPLETED
            );



            
            log.info("Attempting to send OrderCompletedEvent...");
            kafkaTemplate.send("order.command.advance-progress", orderId.toString(), finalEvent);

            log.info("✅ OrderCompletedEvent SENT | orderId={}", orderId);

        } catch (Exception e) {
            log.error("🔥 CRITICAL ERROR in InventoryReservedListener 🔥", e);
            throw e; 
        }
    }
}