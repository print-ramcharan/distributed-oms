package com.oms.sagaorchestrator.saga.messaging;

import com.oms.eventcontracts.events.PaymentCompletedEvent;
import com.oms.sagaorchestrator.saga.domain.OrderSaga;
import com.oms.sagaorchestrator.saga.domain.SagaState;
import com.oms.sagaorchestrator.saga.repository.OrderSagaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentCompletedListener {
    private final OrderSagaRepository sagaRepository;

    @KafkaListener(topics = "payment.completed", groupId = "saga-orchestrator",
    containerFactory = "paymentCompletedKafkaListenerContainerFactory")
    @Transactional
    public void handle(PaymentCompletedEvent event){
        OrderSaga saga = sagaRepository.findById(event.getOrderId()).orElseThrow(() -> new IllegalStateException("Saga not found"));

        if(saga.getState() != SagaState.PAYMENT_INITIATED){
            return;
        }
        saga.markPaymentCompleted();
        sagaRepository.save(saga);
    }
}
