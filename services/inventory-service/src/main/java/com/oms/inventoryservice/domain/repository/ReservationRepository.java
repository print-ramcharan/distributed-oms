package com.oms.inventoryservice.domain.repository;

import com.oms.inventoryservice.domain.model.InventoryReservation;
import com.oms.inventoryservice.domain.model.InventoryReservation.ReservationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {

    Optional<InventoryReservation> findByOrderIdAndProductId(UUID orderId, String productId);

    InventoryReservation save(InventoryReservation reservation);

    List<InventoryReservation> findExpiredReservations(Instant now, ReservationStatus status);

    boolean existsByOrderId(String string);

    void saveAll(List<InventoryReservation> newReservations);
}











