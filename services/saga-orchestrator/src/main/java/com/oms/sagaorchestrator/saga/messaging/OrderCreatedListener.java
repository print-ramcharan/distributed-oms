package com.oms.sagaorchestrator.saga.messaging;

import com.oms.eventcontracts.commands.InitiatePaymentCommand;
import com.oms.eventcontracts.events.OrderCreatedEvent;
import com.oms.sagaorchestrator.saga.domain.OrderSaga;
import com.oms.sagaorchestrator.saga.domain.SagaState;
import com.oms.sagaorchestrator.saga.repository.OrderSagaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCreatedListener {
    private final OrderSagaRepository sagaRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "order.created", groupId = "saga-orchestrator")
    @Transactional
    public void handle(OrderCreatedEvent event){

        OrderSaga saga = sagaRepository.findById(event.getOrderId())
                .orElseGet(() -> sagaRepository.save(new OrderSaga(event.getOrderId())));

        if(saga.getState() != SagaState.STARTED){
            return;
        }

        InitiatePaymentCommand command = new InitiatePaymentCommand(event.getOrderId(), event.getAmount());

        kafkaTemplate.send("payment.initiate", event.getAmount().toString(), command);


        saga.markPaymentRequested();
        sagaRepository.save(saga);
    }
}
