package com.oms.inventoryservice.application;

import com.oms.eventcontracts.commands.ReserveInventoryCommand;
import com.oms.eventcontracts.events.InventoryReservedEvent;
import com.oms.eventcontracts.events.InventoryUnavailableEvent;
import com.oms.inventoryservice.domain.event.InventoryEventPublisher;
import com.oms.inventoryservice.domain.model.Inventory;
import com.oms.inventoryservice.domain.model.InventoryReservation;
import com.oms.inventoryservice.domain.repository.InventoryRepository;
import com.oms.inventoryservice.domain.repository.ReservationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReserveStockUseCase {

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final InventoryEventPublisher eventPublisher;

    @Transactional
    public void execute(UUID orderId, List<ReserveInventoryCommand.LineItem> items) {

        
        
        if (reservationRepository.existsByOrderId(orderId.toString())) {
            log.info("Ignored duplicate reservation request for orderId={}", orderId);
            return;
        }

        
        List<String> productIds = items.stream()
                .map(ReserveInventoryCommand.LineItem::getProductId)
                .toList();

        Map<String, Inventory> inventoryMap = inventoryRepository.findAllByProductIdIn(productIds)
                .stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));

        
        for (ReserveInventoryCommand.LineItem item : items) {
            Inventory inventory = inventoryMap.get(item.getProductId());

            
            if (inventory == null) {
                log.warn("Validation Failed: Product {} not found for order {}", item.getProductId(), orderId);
                publishFailure(orderId, "PRODUCT_NOT_FOUND: " + item.getProductId());
                return;
            }

            
            if (!inventory.hasAvailableStock(item.getQuantity())) {
                log.warn("Validation Failed: Insufficient stock for {} in order {}", item.getProductId(), orderId);
                publishFailure(orderId, "INSUFFICIENT_STOCK: " + item.getProductId());
                return;
            }
        }

        
        
        List<InventoryReservation> newReservations = new ArrayList<>();

        try {
            for (ReserveInventoryCommand.LineItem item : items) {
                Inventory inventory = inventoryMap.get(item.getProductId());

                
                inventory.reserveStock(item.getQuantity());

                
                newReservations.add(new InventoryReservation(
                        orderId,
                        item.getProductId(),
                        item.getQuantity()
                ));
            }

            
            inventoryRepository.saveAll(inventoryMap.values());
            reservationRepository.saveAll(newReservations);

            
            publishSuccess(orderId);

        } catch (Exception e) {
            log.error("Unexpected error during reservation for order {}", orderId, e);
            throw e; 
        }
    }

    private void publishSuccess(UUID orderId) {
        log.info("Inventory successfully reserved for orderId={}", orderId);
        eventPublisher.publishInventoryReserved(
                new InventoryReservedEvent(
                        orderId.toString(),
                        Instant.now()
                )
        );
    }

    private void publishFailure(UUID orderId, String reason) {
        eventPublisher.publishInventoryUnavailable(
                new InventoryUnavailableEvent(
                        orderId.toString(),
                        reason,
                        Instant.now()
                )
        );
    }
}
