package com.oms.inventoryservice.service;

import com.oms.inventoryservice.domain.Inventory;
import com.oms.inventoryservice.domain.InventoryReservation;
import com.oms.inventoryservice.repository.InventoryRepository;
import com.oms.inventoryservice.repository.InventoryReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Transactional
    public InventoryReservation reserveInventory(UUID orderId, String productId, int quantity) {
        // Idempotency: check if reservation already exists
        return reservationRepository.findByOrderId(orderId)
                .orElseGet(() -> {
                    Inventory inventory = inventoryRepository.findById(productId)
                            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

                    if (!inventory.hasAvailableStock(quantity)) {
                        throw new IllegalStateException("Insufficient stock for product: " + productId);
                    }

                    inventory.reserve(quantity);
                    inventoryRepository.save(inventory);

                    InventoryReservation reservation = new InventoryReservation(orderId, productId, quantity);
                    return reservationRepository.save(reservation);
                });
    }

    @Transactional
    public void releaseReservation(UUID orderId) {
        InventoryReservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found for orderId: " + orderId));

        Inventory inventory = inventoryRepository.findById(reservation.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + reservation.getProductId()));

        inventory.release(reservation.getQuantity());
        inventoryRepository.save(inventory);

        reservationRepository.delete(reservation);
    }

    @Transactional
    public void confirmReservation(UUID orderId) {
        InventoryReservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found for orderId: " + orderId));

        Inventory inventory = inventoryRepository.findById(reservation.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + reservation.getProductId()));

        inventory.confirm(reservation.getQuantity());
        reservation.confirm();

        reservationRepository.save(reservation);
        inventoryRepository.save(inventory);
    }

    public boolean isAvailable(String productId, int quantity) {
        return inventoryRepository.findById(productId)
                .map(inventory -> inventory.hasAvailableStock(quantity))
                .orElse(false);
    }
}
