package com.oms.inventoryservice.service;

import com.oms.inventoryservice.domain.model.Inventory;
import com.oms.inventoryservice.domain.model.InventoryReservation;
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
                                                        .orElseThrow(() -> new IllegalArgumentException(
                                                                        "Product not found: " + productId));

                                        // Use domain model method
                                        inventory.reserveStock(quantity);
                                        inventoryRepository.save(inventory);

                                        InventoryReservation reservation = new InventoryReservation(orderId, productId,
                                                        quantity);
                                        return reservationRepository.save(reservation);
                                });
        }

        @Transactional
        public void releaseReservation(UUID orderId) {
                InventoryReservation reservation = reservationRepository.findByOrderId(orderId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Reservation not found for orderId: " + orderId));

                Inventory inventory = inventoryRepository.findById(reservation.getProductId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Product not found: " + reservation.getProductId()));

                // Use domain model method (added recently)
                inventory.releaseStock(reservation.getQuantity());
                // Also update reservation status
                reservation.release();

                inventoryRepository.save(inventory);
                reservationRepository.save(reservation); // Save reservation status change
        }

        @Transactional
        public void confirmReservation(UUID orderId) {
                InventoryReservation reservation = reservationRepository.findByOrderId(orderId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Reservation not found for orderId: " + orderId));

                Inventory inventory = inventoryRepository.findById(reservation.getProductId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Product not found: " + reservation.getProductId()));

                // Use domain model method
                inventory.confirmReservation(reservation.getQuantity());
                reservation.confirm();

                reservationRepository.save(reservation);
                inventoryRepository.save(inventory);
        }

        @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60000)
        @Transactional
        public void releaseExpiredReservations() {
                java.time.Instant now = java.time.Instant.now();
                java.util.List<InventoryReservation> expiredReservations = reservationRepository
                                .findByStatusAndExpiresAtBefore(
                                                InventoryReservation.ReservationStatus.RESERVED, now);

                if (!expiredReservations.isEmpty()) {
                        System.out.println(
                                        "Found " + expiredReservations.size() + " expired reservations. Releasing...");
                        for (InventoryReservation reservation : expiredReservations) {
                                try {
                                        releaseReservation(reservation.getOrderId());
                                } catch (Exception e) {
                                        System.err.println("Failed to release expired reservation: "
                                                        + reservation.getId() + " - " + e.getMessage());
                                }
                        }
                }
        }

        public boolean isAvailable(String productId, int quantity) {
                return inventoryRepository.findById(productId)
                                .map(inventory -> inventory.hasAvailableStock(quantity))
                                .orElse(false);
        }
}
