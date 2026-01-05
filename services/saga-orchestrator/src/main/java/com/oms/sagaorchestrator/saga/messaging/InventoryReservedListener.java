package com.oms.sagaorchestrator.saga.messaging;

import com.oms.eventcontracts.events.InventoryReservedEvent;
import com.oms.eventcontracts.events.OrderCompletedEvent;
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
public class InventoryReservedListener {

    private final OrderSagaRepository sagaRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(
            topics = "${kafka.topics.inventory-reserved}",
            groupId = "${kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handle(
            @Payload InventoryReservedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info(
                "InventoryReservedEvent | orderId={} | topic={} | offset={}",
                event.getOrderId(), topic, offset
        );

        OrderSaga saga = sagaRepository.findById(UUID.fromString(event.getOrderId()))
                .orElseThrow(() -> new IllegalStateException("Saga not found"));

        if (saga.getState() != SagaState.INVENTORY_REQUESTED) {
            log.warn("Ignoring InventoryReservedEvent in state {}", saga.getState());
            return;
        }

        saga.markCompleted();
        sagaRepository.save(saga);

        // Final success signal
        kafkaTemplate.send(
                "order.completed",
                String.valueOf(saga.getOrderId()),
                new OrderCompletedEvent(saga.getOrderId(), Instant.now())
        );
    }
}

