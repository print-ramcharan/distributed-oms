package com.oms.inventoryservice.infrastructure.kafka;

import com.oms.eventcontracts.events.PaymentCompletedEvent;
import com.oms.inventoryservice.application.ReserveStockUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedConsumer {

    private final ReserveStockUseCase reserveStockUseCase;

    @KafkaListener(
            topics = "${kafka.topics.payment-completed}",
            groupId = "${kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentCompleted(
            @Payload PaymentCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info(
                "Received PaymentCompletedEvent | orderId={} | topic={} | offset={}",
                event.getOrderId(), topic, offset
        );

        try {
            // TEMPORARY SIMPLIFICATION:
            // PaymentCompletedEvent does not yet include item details.
            // This will be fixed during Saga enhancement.
            String productId = "p1";
            int quantity = 1;

            reserveStockUseCase.execute(
                    event.getOrderId().toString(),
                    productId,
                    quantity
            );

        } catch (Exception e) {
            log.error(
                    "Error processing PaymentCompletedEvent | orderId={}",
                    event.getOrderId(),
                    e
            );
            throw e; // Trigger retry / DLQ
        }
    }
}
