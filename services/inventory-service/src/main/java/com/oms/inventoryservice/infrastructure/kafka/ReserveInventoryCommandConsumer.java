package com.oms.inventoryservice.infrastructure.kafka;

import com.oms.eventcontracts.commands.ReserveInventoryCommand;
import com.oms.inventoryservice.application.ReserveStockUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReserveInventoryCommandConsumer {

    private final ReserveStockUseCase reserveStockUseCase;

    @KafkaListener(
            topics = "inventory.reserve.command",
            groupId = "inventory-service"
    )
    public void handle(ReserveInventoryCommand command) {

        log.info(
                "ReserveInventoryCommand | orderId={} | productId={} | qty={}",
                command.getOrderId(),
                command.getProductId(),
                command.getQuantity()
        );

        // 🔥 The use-case already handles:
        // - Idempotency
        // - Product not found
        // - Insufficient stock
        // - Publishing InventoryReserved / InventoryUnavailable events
        // - NO exceptions for business failures
        reserveStockUseCase.execute(
                command.getOrderId(),
                command.getProductId(),
                command.getQuantity()
        );
    }
}


//package com.oms.inventoryservice.infrastructure.kafka;
//
//import com.oms.eventcontracts.events.InventoryReserveRequestedEvent;
//import com.oms.eventcontracts.events.InventoryReservedEvent;
//import com.oms.eventcontracts.events.InventoryUnavailableEvent;
//import com.oms.inventoryservice.application.ReserveStockUseCase;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.support.KafkaHeaders;
//import org.springframework.messaging.handler.annotation.Header;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.stereotype.Component;
//
//import java.time.Instant;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class InventoryReserveRequestedConsumer {
//
//    private final ReserveStockUseCase reserveStockUseCase;
//
//    @KafkaListener(
//            topics = "${kafka.topics.inventory-reserve-requested}",
//            groupId = "${kafka.consumer.group-id}",
//            containerFactory = "kafkaListenerContainerFactory"
//    )
//    public void handle(
//            @Payload InventoryReserveRequestedEvent event,
//            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
//            @Header(KafkaHeaders.OFFSET) long offset
//    ) {
//        log.info(
//                "InventoryReserveRequestedEvent | orderId={} | productId={} | qty={} | topic={} | offset={}",
//                event.getOrderId(),
//                event.getProductId(),
//                event.getQuantity(),
//                topic,
//                offset
//        );
//
//        // 🚨 Never throw for business failures
//        reserveStockUseCase.execute(
//                event.getOrderId(),
//                event.getProductId(),
//                event.getQuantity()
//        );
//    }
//}
