package com.oms.inventoryservice.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "inventory_reservations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "product_id"})
)
@Getter
@NoArgsConstructor
public class InventoryReservation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false)
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

    public InventoryReservation(UUID orderId, String productId, int quantity) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = ReservationStatus.RESERVED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.expiresAt = this.createdAt.plusSeconds(900);
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
        RESERVED, CONFIRMED, RELEASED
    }
}
