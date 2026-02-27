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
                try {
                        UUID orderId = UUID.fromString(event.getOrderId());

                        log.info("🔴 InventoryUnavailableEvent received | orderId={} | reason={}",
                                        orderId, event.getReason());

                        
                        OrderSaga saga = sagaRepository.findById(orderId).orElse(null);
                        if (saga == null) {
                                log.warn("⚠️  Saga not found for order {}, ignoring event", orderId);
                                return;
                        }
                        log.info("✓ Saga loaded | currentState={}", saga.getState());

                        
                        if (saga.getState() != SagaState.INVENTORY_REQUESTED) {
                                log.warn("⚠️  Ignoring event - saga in wrong state | orderId={} | currentState={} | expectedState=INVENTORY_REQUESTED",
                                                orderId, saga.getState());
                                return;
                        }

                        
                        log.info("→ Transitioning saga to INVENTORY_FAILED...");
                        saga.markInventoryFailed();
                        sagaRepository.save(saga);
                        log.info("✓ Saga transitioned to INVENTORY_FAILED");

                        log.info("→ Transitioning saga to COMPENSATING...");
                        saga.markCompensating();
                        sagaRepository.save(saga);
                        log.info("✓ Saga transitioned to COMPENSATING");

                        
                        log.info("→ Sending RefundPaymentCommand...");
                        var refundCommand = new RefundPaymentCommand(
                                        orderId,
                                        saga.getAmount(),
                                        event.getReason(),
                                        Instant.now());

                        var refundResult = kafkaTemplate.send(
                                        "payment.refund.command",
                                        orderId.toString(),
                                        refundCommand).get(); 

                        log.info("✓ RefundPaymentCommand sent successfully | recordMetadata={}",
                                        refundResult.getRecordMetadata());

                        
                        log.info("→ Sending AdvanceOrderProgressCommand(ORDER_FAILED)...");
                        var orderFailCommand = new AdvanceOrderProgressCommand(
                                        orderId,
                                        OrderProgress.ORDER_FAILED);

                        var orderFailResult = kafkaTemplate.send(
                                        "order.command.advance-progress",
                                        orderId.toString(),
                                        orderFailCommand).get(); 

                        log.info("✓ AdvanceOrderProgressCommand sent successfully | recordMetadata={}",
                                        orderFailResult.getRecordMetadata());

                        log.info("✅ Rollback compensation completed successfully | orderId={}", orderId);

                } catch (IllegalStateException e) {
                        log.error("❌ Invalid state transition in saga | error={}", e.getMessage(), e);
                        throw new RuntimeException("Saga state transition failed", e);
                } catch (Exception e) {
                        log.error("❌ Failed to process InventoryUnavailableEvent | orderId={} | error={}",
                                        event.getOrderId(), e.getMessage(), e);
                        throw new RuntimeException("Rollback compensation failed", e);
                }
        }
}
