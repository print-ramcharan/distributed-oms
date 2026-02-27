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

        @Autowired
        private org.redisson.api.RedissonClient redissonClient;

        @Transactional
        public InventoryReservation reserveInventory(UUID orderId, String productId, int quantity) {

                return reservationRepository.findByOrderId(orderId)
                                .orElseGet(() -> {
                                        org.redisson.api.RLock lock = redissonClient
                                                        .getLock("inventory:lock:" + productId);
                                        try {
                                                boolean acquired = lock.tryLock(10, 30,
                                                                java.util.concurrent.TimeUnit.SECONDS);
                                                if (!acquired) {
                                                        throw new RuntimeException(
                                                                        "Could not acquire lock for product: "
                                                                                        + productId);
                                                }

                                                try {
                                                        Inventory inventory = inventoryRepository.findById(productId)
                                                                        .orElseThrow(() -> new IllegalArgumentException(
                                                                                        "Product not found: "
                                                                                                        + productId));

                                                        inventory.reserveStock(quantity);
                                                        inventoryRepository.save(inventory);

                                                        InventoryReservation reservation = new InventoryReservation(
                                                                        orderId, productId,
                                                                        quantity);
                                                        return reservationRepository.save(reservation);
                                                } finally {
                                                        lock.unlock();
                                                }
                                        } catch (InterruptedException e) {
                                                Thread.currentThread().interrupt();
                                                throw new RuntimeException("Lock acquisition interrupted", e);
                                        }
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

                inventory.releaseStock(reservation.getQuantity());

                reservation.release();

                inventoryRepository.save(inventory);
                reservationRepository.save(reservation);
        }

        @Transactional
        public void confirmReservation(UUID orderId) {
                InventoryReservation reservation = reservationRepository.findByOrderId(orderId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Reservation not found for orderId: " + orderId));

                Inventory inventory = inventoryRepository.findById(reservation.getProductId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Product not found: " + reservation.getProductId()));

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

        @Transactional
        public void addStock(String productId, int quantity) {
                Inventory inventory = inventoryRepository.findById(productId)
                                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
                inventory.addStock(quantity);
                inventoryRepository.save(inventory);
        }

        public java.util.List<Inventory> findAll() {
                return inventoryRepository.findAll();
        }
}
