package com.oms.sagaorchestrator.saga.messaging;

import com.oms.eventcontracts.commands.AdvanceOrderProgressCommand;
import com.oms.eventcontracts.commands.RefundPaymentCommand;
import com.oms.eventcontracts.enums.OrderProgress;
import com.oms.eventcontracts.events.InventoryUnavailableEvent;
import com.oms.sagaorchestrator.saga.domain.OrderSaga;
import com.oms.sagaorchestrator.saga.domain.SagaState;
import com.oms.sagaorchestrator.saga.repository.OrderSagaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryUnavailableListener {

        private final OrderSagaRepository sagaRepository;
        private final KafkaTemplate<String, Object> kafkaTemplate;

        @KafkaListener(topics = "${kafka.topics.inventory-unavailable}", containerFactory = "inventoryUnavailableKafkaListenerContainerFactory", groupId = "${kafka.consumer.group-id}")
        @Transactional
        public void handle(InventoryUnavailableEvent event) {

                UUID orderId = UUID.fromString(event.getOrderId());

                log.info("InventoryUnavailable | orderId={} | reason={}",
                                orderId, event.getReason());

                OrderSaga saga = sagaRepository.findById(orderId).orElse(null);

                // ✅ Saga may not exist — this is NOT an error
                if (saga == null) {
                        log.warn("Saga not found for order {}, ignoring", orderId);
                        return;
                }

                // ✅ Idempotency guard
                if (saga.getState() != SagaState.INVENTORY_REQUESTED) {
                        log.warn("Ignoring InventoryUnavailable for order {} in state {}",
                                        orderId, saga.getState());
                        return;
                }

                // 1️⃣ Compensate
                saga.markCompensating();
                sagaRepository.save(saga);

                // 2️⃣ Refund payment
                kafkaTemplate.send(
                                "payment.refund.command",
                                orderId.toString(),
                                new RefundPaymentCommand(
                                                orderId,
                                                saga.getAmount(),
                                                event.getReason(),
                                                Instant.now()));

                // 3️⃣ Fail order
                kafkaTemplate.send(
                                "order.command.advance-progress",
                                orderId.toString(),
                                new AdvanceOrderProgressCommand(
                                                orderId,
                                                OrderProgress.ORDER_FAILED));
        }
}
