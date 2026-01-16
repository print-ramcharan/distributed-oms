package com.oms.orderservice.infrastructure.messaging;


import com.oms.eventcontracts.commands.AdvanceOrderProgressCommand;
import com.oms.orderservice.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdvanceOrderProgressCommandConsumer {

    private final OrderRepository orderRepository;

    @KafkaListener(
            topics = "order.command.advance-progress",
            groupId = "order-service"
    )
    @Transactional
    public void handle(
            AdvanceOrderProgressCommand command,
            Acknowledgment ack
    ) {
        try {
            log.info(
                    "➡️ AdvanceOrderProgressCommand received: orderId={}, target={}",
                    command.getOrderId(),
                    command.getTargetProgress()
            );

            orderRepository.findById(command.getOrderId())
                    .ifPresent(order ->
                            order.advanceProgress(command.getTargetProgress())
                    );

            ack.acknowledge();

        } catch (Exception ex) {
            log.error("❌ Failed to handle AdvanceOrderProgressCommand", ex);
            throw ex; // let Kafka retry / DLQ
        }
    }
}

