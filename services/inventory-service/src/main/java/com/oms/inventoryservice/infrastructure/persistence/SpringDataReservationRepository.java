package com.oms.inventoryservice.infrastructure.persistence;

import com.oms.inventoryservice.domain.model.InventoryReservation;
import com.oms.inventoryservice.domain.model.InventoryReservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface SpringDataReservationRepository
        extends JpaRepository<InventoryReservation, UUID> {

    Optional<InventoryReservation> findByOrderIdAndProductId(UUID orderId, String productId);

    List<InventoryReservation> findByExpiresAtBeforeAndStatus(
            Instant now,
            ReservationStatus status
    );
}
