package com.oms.sagaorchestrator.saga.messaging;

import com.oms.eventcontracts.commands.RefundPaymentCommand;
import com.oms.eventcontracts.events.InventoryUnavailableEvent;
import com.oms.eventcontracts.events.OrderCancelledEvent;
import com.oms.sagaorchestrator.saga.domain.OrderSaga;
import com.oms.sagaorchestrator.saga.domain.SagaState;
import com.oms.sagaorchestrator.saga.repository.OrderSagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
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
//            containerFactory = "genericKafkaListenerContainerFactory"
    )
    public void handle(InventoryUnavailableEvent event, Acknowledgment ack) {

        UUID orderId = UUID.fromString(event.getOrderId());

        log.info("InventoryUnavailable | orderId={} | reason={}",
                orderId, event.getReason());

        OrderSaga saga = sagaRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Saga not found for " + orderId));

        // Idempotency
        if (saga.getState() != SagaState.INVENTORY_REQUESTED) {
            log.warn("Ignoring InventoryUnavailable for order {} in state {}",
                    orderId, saga.getState());
            ack.acknowledge();
            return;
        }

        // 1️⃣ Move saga to compensating
        saga.markCompensating();
        sagaRepository.save(saga);

        // 2️⃣ Send refund command to Payment Service
        kafkaTemplate.send(
                "payment.refund.command",
                orderId.toString(),
                new RefundPaymentCommand(
                        orderId,
                        saga.getAmount(),
                        event.getReason(),
                        Instant.now()
                )
        );

        // 3️⃣ Cancel the order
        kafkaTemplate.send(
                "order.cancelled",
                orderId.toString(),
                new OrderCancelledEvent(
                        orderId,
                        "Inventory unavailable",
                        Instant.now()
                )
        );

        ack.acknowledge();
    }
}
