package com.oms.inventoryservice.service;

import com.oms.inventoryservice.domain.model.Inventory;
import com.oms.inventoryservice.domain.model.InventoryReservation;
import com.oms.inventoryservice.repository.InventoryRepository;
import com.oms.inventoryservice.repository.InventoryReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
public class InventoryService {

        @Autowired
        private InventoryRepository inventoryRepository;

        @Autowired
        private InventoryReservationRepository reservationRepository;

        @Autowired
        private StringRedisTemplate redisTemplate;

        /**
         * Acquire a simple Redis SET NX lock. Replaces Redisson RLock
         * (Redisson 3.x is incompatible with Spring Boot 4.0).
         */
        private boolean acquireLock(String key, String value, Duration ttl) {
                Boolean acquired = redisTemplate.opsForValue()
                                .setIfAbsent(key, value, ttl);
                return Boolean.TRUE.equals(acquired);
        }

        private void releaseLock(String key, String value) {
                String current = redisTemplate.opsForValue().get(key);
                if (value.equals(current)) {
                        redisTemplate.delete(key);
                }
        }

        @Transactional
        public InventoryReservation reserveInventory(UUID orderId, String productId, int quantity) {
                return reservationRepository.findByOrderId(orderId)
                                .orElseGet(() -> {
                                        String lockKey = "inventory:lock:" + productId;
                                        String lockValue = orderId.toString();
                                        boolean acquired = false;
                                        int attempts = 0;
                                        while (!acquired && attempts < 10) {
                                                acquired = acquireLock(lockKey, lockValue, Duration.ofSeconds(30));
                                                if (!acquired) {
                                                        try {
                                                                Thread.sleep(100);
                                                        } catch (InterruptedException e) {
                                                                Thread.currentThread().interrupt();
                                                                throw new RuntimeException("Lock interrupted", e);
                                                        }
                                                }
                                                attempts++;
                                        }
                                        if (!acquired) {
                                                throw new RuntimeException(
                                                                "Could not acquire lock for product: " + productId);
                                        }
                                        try {
                                                Inventory inventory = inventoryRepository.findById(productId)
                                                                .orElseThrow(() -> new IllegalArgumentException(
                                                                                "Product not found: " + productId));
                                                inventory.reserveStock(quantity);
                                                inventoryRepository.save(inventory);
                                                InventoryReservation reservation = new InventoryReservation(orderId,
                                                                productId, quantity);
                                                return reservationRepository.save(reservation);
                                        } finally {
                                                releaseLock(lockKey, lockValue);
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
                java.util.List<InventoryReservation> expired = reservationRepository
                                .findByStatusAndExpiresAtBefore(InventoryReservation.ReservationStatus.RESERVED, now);
                if (!expired.isEmpty()) {
                        System.out.println("Releasing " + expired.size() + " expired reservations");
                        for (InventoryReservation r : expired) {
                                try {
                                        releaseReservation(r.getOrderId());
                                } catch (Exception e) {
                                        System.err.println("Failed to release: " + r.getId() + " - " + e.getMessage());
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
