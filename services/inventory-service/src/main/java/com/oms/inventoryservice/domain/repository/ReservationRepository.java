package com.oms.inventoryservice.domain.repository;

import com.oms.inventoryservice.domain.model.InventoryReservation;
import com.oms.inventoryservice.domain.model.InventoryReservation.ReservationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {

    Optional<InventoryReservation> findByOrderId(String orderId);

    InventoryReservation save(InventoryReservation reservation);

    List<InventoryReservation> findExpiredReservations(Instant now, ReservationStatus status);
}
