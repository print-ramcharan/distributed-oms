package com.oms.inventoryservice.infrastructure.kafka;

import com.oms.eventcontracts.events.InventoryReserveRequestedEvent;
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
public class InventoryReserveRequestedConsumer {

    private final ReserveStockUseCase reserveStockUseCase;

    @KafkaListener(
            topics = "${kafka.topics.inventory-reserve-requested}",
            groupId = "${kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handle(
            @Payload InventoryReserveRequestedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info(
                "Received InventoryReserveRequestedEvent | orderId={} | productId={} | qty={} | topic={} | offset={}",
                event.getOrderId(),
                event.getProductId(),
                event.getQuantity(),
                topic,
                offset
        );

        try {
            reserveStockUseCase.execute(
                    event.getOrderId().toString(),
                    event.getProductId(),
                    event.getQuantity()
            );
        } catch (Exception e) {
            log.error(
                    "Inventory reservation failed | orderId={}",
                    event.getOrderId(),
                    e
            );
            throw e; // retry → DLQ
        }
    }
}
