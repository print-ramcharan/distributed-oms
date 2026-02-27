package com.oms.orderservice.infrastructure.messaging;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.eventcontracts.commands.AdvanceOrderProgressCommand;
import com.oms.eventcontracts.events.OrderProgressUpdatedEvent;
import com.oms.orderservice.domain.outbox.OutboxEvent;
import com.oms.orderservice.domain.outbox.OutboxRepository;
import com.oms.orderservice.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.oms.orderservice.domain.outbox.AggregateType.ORDER;


@Component
@RequiredArgsConstructor
@Slf4j
public class AdvanceOrderProgressCommandConsumer {

    private final OrderRepository orderRepository;

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;


    @KafkaListener(
            topics = "order.command.advance-progress",
            groupId = "order-service",
            containerFactory = "advanceOrderProgressKafkaListenerContainerFactory"
    )

    @Transactional
    public void handle(
            AdvanceOrderProgressCommand command,
            Acknowledgment ack
    ){
        try {
            log.info(
                    "➡️ AdvanceOrderProgressCommand received: orderId={}, target={}",
                    command.getOrderId(),
                    command.getTargetProgress()
            );

            var order = orderRepository.findById(command.getOrderId())
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Order not found: " + command.getOrderId()
                            )
                    );

            order.advanceProgress(command.getTargetProgress());

            OrderProgressUpdatedEvent event =
                    new OrderProgressUpdatedEvent(
                            order.getId(),
                            order.getProgress()
                    );


            String payload;
            try {
                payload = objectMapper.writeValueAsString(event);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to serialize OrderProgressUpdatedEvent for order " + order.getId(),
                        e
                );
            }

            OutboxEvent outboxEvent = OutboxEvent.create(
                    order.getId(),
                    ORDER,
                    OrderProgressUpdatedEvent.class.getSimpleName(),
                    payload
            );

            outboxRepository.save(outboxEvent);

            
            ack.acknowledge();

        } catch (Exception ex) {
            log.error("❌ Failed to handle AdvanceOrderProgressCommand", ex);
            throw ex;
        }
    }
}


