package com.oms.sagaorchestrator.saga.messaging;

import com.oms.eventcontracts.commands.AdvanceOrderProgressCommand;
import com.oms.eventcontracts.enums.OrderProgress;
import com.oms.eventcontracts.events.PaymentFailedEvent;
import com.oms.sagaorchestrator.saga.domain.OrderSaga;
import com.oms.sagaorchestrator.saga.domain.SagaState;
import com.oms.sagaorchestrator.saga.repository.OrderSagaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentFailedListener {
    private final OrderSagaRepository sagaRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;


    @KafkaListener(topics = "payment.failed", groupId = "saga-orchestrator"
//            , containerFactory = "genericKafkaListenerContainerFactory"
            )
    public void handle(PaymentFailedEvent event){
        OrderSaga saga = sagaRepository.findById(event.getOrderId()).orElseThrow(() -> new IllegalStateException("Saga not found"));

        if(saga.getState() != SagaState.PAYMENT_REQUESTED){
            return;
        }

        kafkaTemplate.send(
                "order.command.advance-progress",
                String.valueOf(event.getOrderId()),
                new AdvanceOrderProgressCommand(
                        event.getOrderId(),
                        OrderProgress.ORDER_FAILED
                )

        );

        saga.markPaymentFailed();
        sagaRepository.save(saga);
    }
}
