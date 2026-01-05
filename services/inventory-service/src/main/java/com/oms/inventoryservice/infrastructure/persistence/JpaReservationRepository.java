package com.oms.inventoryservice.infrastructure.persistence;

import com.oms.inventoryservice.domain.model.InventoryReservation;
import com.oms.inventoryservice.domain.model.InventoryReservation.ReservationStatus;
import com.oms.inventoryservice.domain.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaReservationRepository implements ReservationRepository {

    private final com.oms.inventoryservice.infrastructure.persistence.SpringDataReservationRepository springRepo;

    @Override
    public Optional<InventoryReservation> findByOrderId(String orderId) {
        return springRepo.findByOrderId(orderId);
    }

    @Override
    public InventoryReservation save(InventoryReservation reservation) {
        return springRepo.save(reservation);
    }

    @Override
    public List<InventoryReservation> findExpiredReservations(Instant now, ReservationStatus status) {
        return springRepo.findExpiredReservations(now, status);
    }
}
