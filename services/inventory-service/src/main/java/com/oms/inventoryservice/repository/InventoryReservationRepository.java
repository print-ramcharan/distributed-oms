package com.oms.inventoryservice.repository;

import com.oms.inventoryservice.domain.model.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {
    Optional<InventoryReservation> findByOrderId(UUID orderId);

    java.util.List<InventoryReservation> findByStatusAndExpiresAtBefore(InventoryReservation.ReservationStatus status,
            java.time.Instant expiresAt);
}
