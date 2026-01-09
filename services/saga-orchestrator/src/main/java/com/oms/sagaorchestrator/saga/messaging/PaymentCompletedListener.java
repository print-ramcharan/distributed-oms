package com.oms.sagaorchestrator.saga.messaging;

import com.oms.eventcontracts.commands.ReserveInventoryCommand;
import com.oms.eventcontracts.events.PaymentCompletedEvent;
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
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedListener {

    private final OrderSagaRepository sagaRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(
            topics = "payment.completed",
            groupId = "saga-orchestrator",
            containerFactory = "paymentCompletedKafkaListenerContainerFactory"
    )
    @Transactional
    public void handle(PaymentCompletedEvent event) {

        UUID orderId = event.getOrderId();
        OrderSaga saga = sagaRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Saga not found"));

        if (saga.getState() != SagaState.PAYMENT_REQUESTED) return;

        // 1. Update State
        saga.markPaymentCompleted();
        saga.markInventoryRequested();
        sagaRepository.save(saga);

        List<ReserveInventoryCommand.LineItem> commandItems = saga.getItems().stream()
                .map(item -> new ReserveInventoryCommand.LineItem(item.getProductId(), item.getQuantity()))
                .toList();

// Create ONE command
        ReserveInventoryCommand command = new ReserveInventoryCommand(
                orderId.toString(),
                commandItems,
                Instant.now()
        );
        // 3. Send ONE message
        kafkaTemplate.send("inventory.reserve.command", orderId.toString(), command);

        log.info("Batch ReserveInventoryCommand sent for orderId={}", orderId);
    }
}