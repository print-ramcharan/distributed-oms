package com.oms.sagaorchestrator.saga.messaging;

import com.oms.eventcontracts.events.InventoryReserveRequestedEvent;
import com.oms.eventcontracts.events.PaymentCompletedEvent;
import com.oms.sagaorchestrator.saga.domain.OrderSaga;
import com.oms.sagaorchestrator.saga.domain.SagaState;
import com.oms.sagaorchestrator.saga.repository.OrderSagaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class PaymentCompletedListener {

    private final OrderSagaRepository sagaRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(
            topics = "payment.completed",
            groupId = "saga-orchestrator",
            containerFactory = "paymentCompletedKafkaListenerContainerFactory"
    )


    @KafkaListener(
            topics = "payment.completed",
            groupId = "saga-orchestrator",
            containerFactory = "paymentCompletedKafkaListenerContainerFactory"
    )
    @Transactional
    public void handle(PaymentCompletedEvent event) {

        OrderSaga saga = sagaRepository.findById(event.getOrderId()).orElse(null);

        if (saga == null) {
            return;
        }

        if (saga.getState() != SagaState.PAYMENT_INITIATED) {
            return;
        }

        saga.markPaymentCompleted();
        sagaRepository.save(saga);

        InventoryReserveRequestedEvent inventoryCommand =
                new InventoryReserveRequestedEvent(
                        event.getOrderId().toString(),
                        "p1",
                        1,
                        Instant.now()
                );

        kafkaTemplate.send(
                "inventory.reserve.requested",
                event.getOrderId().toString(),
                inventoryCommand
        );
    }

}
