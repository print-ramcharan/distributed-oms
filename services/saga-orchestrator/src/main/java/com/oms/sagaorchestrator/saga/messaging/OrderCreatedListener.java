package com.oms.sagaorchestrator.saga.messaging;

import com.oms.eventcontracts.commands.InitiatePaymentCommand;
import com.oms.eventcontracts.events.OrderCreatedEvent;
import com.oms.eventcontracts.events.OrderItemDTO;
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

    @KafkaListener(topics = "order.created", groupId = "saga-orchestrator"
//            containerFactory = "genericKafkaListenerContainerFactory"
            )
    @Transactional
    public void handle(OrderCreatedEvent event) {

        OrderSaga saga = sagaRepository.findById(event.getOrderId())
                .orElseGet(() -> {
                    OrderSaga s = new OrderSaga(event.getOrderId(), event.getAmount());

                    // 🔥 copy items from order event into saga
                    for (OrderItemDTO item : event.getItems()) {
                        s.addItem(
                                item.getProductId(),
                                item.getQuantity(),
                                item.getPrice()
                        );
                    }

                    return sagaRepository.save(s);
                });

        // Idempotency
        if (saga.getState() != SagaState.STARTED) {
            return;
        }

        // 1️⃣ Send PAYMENT command
        InitiatePaymentCommand command =
                new InitiatePaymentCommand(event.getOrderId(), event.getAmount());

        kafkaTemplate.send(
                "payment.initiate",
                event.getOrderId().toString(),
                command
        );

        // 2️⃣ Move saga forward
        saga.markPaymentRequested();
        sagaRepository.save(saga);
    }
}
