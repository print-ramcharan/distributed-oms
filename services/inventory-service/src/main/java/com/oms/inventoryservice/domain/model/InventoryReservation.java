package com.oms.inventoryservice.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations")
@Getter
@NoArgsConstructor
public class InventoryReservation {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public InventoryReservation(String orderId, String productId, int quantity) {
        this.id = UUID.randomUUID().toString();
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = ReservationStatus.RESERVED;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.expiresAt = Instant.now().plusSeconds(900);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void confirm() {
        if (status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("Can only confirm RESERVED reservations");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.updatedAt = Instant.now();
    }

    public void release() {
        if (status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("Can only release RESERVED reservations");
        }
        this.status = ReservationStatus.RELEASED;
        this.updatedAt = Instant.now();
    }

    public enum ReservationStatus {
        RESERVED,
        CONFIRMED,
        RELEASED
    }
}
