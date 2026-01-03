package com.oms.sagaorchestrator.saga.messaging;

import com.oms.eventcontracts.events.PaymentFailedEvent;
import com.oms.sagaorchestrator.saga.domain.OrderSaga;
import com.oms.sagaorchestrator.saga.domain.SagaState;
import com.oms.sagaorchestrator.saga.repository.OrderSagaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentFailedListener {
    private final OrderSagaRepository sagaRepository;

    @KafkaListener(topics = "payment.failed", groupId = "saga-orchestrator")
    public void handle(PaymentFailedEvent event){
        OrderSaga saga = sagaRepository.findById(event.getOrderId()).orElseThrow(() -> new IllegalStateException("Saga not found"));

        if(saga.getState() != SagaState.PAYMENT_INITIATED){
            return;
        }

        saga.markPaymentFailed();
        sagaRepository.save(saga);
    }
}
