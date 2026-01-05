package com.oms.inventoryservice.application;

import com.oms.inventoryservice.domain.event.InventoryEventPublisher;
import com.oms.eventcontracts.events.InventoryReservedEvent;
import com.oms.eventcontracts.events.InventoryUnavailableEvent;
import com.oms.inventoryservice.domain.model.Inventory;
import com.oms.inventoryservice.domain.model.InventoryReservation;
import com.oms.inventoryservice.domain.repository.InventoryRepository;
import com.oms.inventoryservice.domain.repository.ReservationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReserveStockUseCase {

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final InventoryEventPublisher eventPublisher;

    @Transactional
    public void execute(String orderId, String productId, int quantity) {

        // Idempotency
        if (reservationRepository.findByOrderId(orderId).isPresent()) {
            return;
        }

        Inventory inventory = inventoryRepository
                .findByProductId(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found: " + productId)
                );

        try {
            inventory.reserveStock(quantity);
            inventoryRepository.save(inventory);

            InventoryReservation reservation =
                    new InventoryReservation(orderId, productId, quantity);
            reservationRepository.save(reservation);

            eventPublisher.publishInventoryReserved(
                    new InventoryReservedEvent(
                            orderId,
                            productId,
                            quantity,
                            Instant.now()
                    )
            );

        } catch (Inventory.InsufficientStockException ex) {

            eventPublisher.publishInventoryUnavailable(
                    new InventoryUnavailableEvent(
                            orderId,
                            productId,
                            quantity,
                            "Insufficient stock",
                            Instant.now()
                    )
            );
        }
    }

    public static class ProductNotFoundException extends RuntimeException {
        public ProductNotFoundException(String message) {
            super(message);
        }
    }
}
