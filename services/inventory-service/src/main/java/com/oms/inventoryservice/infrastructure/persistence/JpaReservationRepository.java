package com.oms.inventoryservice.infrastructure.persistence;

import com.oms.inventoryservice.domain.model.InventoryReservation;
import com.oms.inventoryservice.domain.model.InventoryReservation.ReservationStatus;
import com.oms.inventoryservice.domain.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
@RequiredArgsConstructor
public class JpaReservationRepository implements ReservationRepository {

    private final SpringDataReservationRepository springRepo;

    @Override
    public Optional<InventoryReservation> findByOrderIdAndProductId(UUID orderId, String productId) {
        return springRepo.findByOrderIdAndProductId(orderId, productId);
    }

    @Override
    public InventoryReservation save(InventoryReservation reservation) {
        return springRepo.save(reservation);
    }

    @Override
    public List<InventoryReservation> findExpiredReservations(Instant now, ReservationStatus status) {
        return springRepo.findByExpiresAtBeforeAndStatus(now, status);
    }

    @Override
    public boolean existsByOrderId(String string) {
        return springRepo.existsById(UUID.fromString(string));
    }

    @Override
    public void saveAll(List<InventoryReservation> newReservations) {
        springRepo.saveAll(newReservations);
    }
}

//@Repository
//@RequiredArgsConstructor
//public class JpaReservationRepository implements ReservationRepository {
//
//    private final com.oms.inventoryservice.infrastructure.persistence.SpringDataReservationRepository springRepo;
//
//    @Override
//    public Optional<InventoryReservation> findByOrderId(String orderId) {
//        return springRepo.findByOrderId(orderId);
//    }
//
//    @Override
//    public InventoryReservation save(InventoryReservation reservation) {
//        return springRepo.save(reservation);
//    }
//
//    @Override
//    public List<InventoryReservation> findExpiredReservations(Instant now, ReservationStatus status) {
//        return springRepo.findExpiredReservations(now, status);
//    }
//}
