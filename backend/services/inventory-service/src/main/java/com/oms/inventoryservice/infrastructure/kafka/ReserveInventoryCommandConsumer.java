package com.oms.inventoryservice.infrastructure.kafka;

import com.oms.eventcontracts.commands.ReserveInventoryCommand;
import com.oms.inventoryservice.application.ReserveStockUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;


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

        log.info("Batch ReserveInventoryCommand received | orderId={} | itemsCount={}",
                command.getOrderId(),
                command.getItems().size()); 

        reserveStockUseCase.execute(
                UUID.fromString(command.getOrderId()),
                command.getItems()
        );
    }
}



































