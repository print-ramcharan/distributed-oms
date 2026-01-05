package com.oms.sagaorchestrator.saga.messaging;

import com.oms.eventcontracts.events.InventoryUnavailableEvent;
import com.oms.eventcontracts.events.OrderCancelledEvent;
import com.oms.sagaorchestrator.saga.domain.OrderSaga;
import com.oms.sagaorchestrator.saga.domain.SagaState;
import com.oms.sagaorchestrator.saga.repository.OrderSagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryUnavailableListener {

    private final OrderSagaRepository sagaRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(
            topics = "${kafka.topics.inventory-unavailable}",
            groupId = "${kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handle(
            @Payload InventoryUnavailableEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info(
                "InventoryUnavailableEvent | orderId={} | reason={}",
                event.getOrderId(), event.getReason()
        );

        OrderSaga saga = sagaRepository.findById(UUID.fromString(event.getOrderId()))
                .orElseThrow(() -> new IllegalStateException("Saga not found"));

        if (saga.getState() != SagaState.INVENTORY_REQUESTED) {
            log.warn("Ignoring InventoryUnavailableEvent in state {}", saga.getState());
            return;
        }

        saga.markCompensating();
        sagaRepository.save(saga);

        // Compensation path (future: payment refund)
        kafkaTemplate.send(
                "order.cancelled",
                String.valueOf(saga.getOrderId()),
                new OrderCancelledEvent(
                        saga.getOrderId(),
                        "Inventory unavailable",
                        Instant.now()
                )
        );
    }
}
