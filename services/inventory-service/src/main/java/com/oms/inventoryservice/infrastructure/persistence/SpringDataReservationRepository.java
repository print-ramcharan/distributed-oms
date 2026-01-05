package com.oms.inventoryservice.infrastructure.persistence;

import com.oms.inventoryservice.domain.model.InventoryReservation;
import com.oms.inventoryservice.domain.model.InventoryReservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SpringDataReservationRepository
        extends JpaRepository<InventoryReservation, String> {

    Optional<InventoryReservation> findByOrderId(String orderId);

    @Query("""
        SELECT r FROM InventoryReservation r
        WHERE r.expiresAt < :now AND r.status = :status
    """)
    List<InventoryReservation> findExpiredReservations(
            @Param("now") Instant now,
            @Param("status") ReservationStatus status
    );
}
