package com.oms.inventoryservice.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.time.Instant;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Inventory {

    // Aggregate identity: one inventory row per product
    @Id
    private String productId;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int availableQuantity;

    @Column(nullable = false)
    private int reservedQuantity;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Inventory(String productId, int initialStock) {
        if (initialStock < 0) {
            throw new IllegalArgumentException("Initial stock cannot be negative");
        }
        this.productId = productId;
        this.totalQuantity = initialStock;
        this.availableQuantity = initialStock;
        this.reservedQuantity = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean hasAvailableStock(int quantity) {
        return availableQuantity >= quantity;
    }

    public void reserveStock(int quantity) {
        if (!hasAvailableStock(quantity)) {
            throw new InsufficientStockException(
                    String.format(
                            "Insufficient stock for product %s. Available=%d, Requested=%d",
                            productId, availableQuantity, quantity));
        }

        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
        this.updatedAt = Instant.now();
    }

    public void confirmReservation(int quantity) {
        if (quantity > reservedQuantity) {
            throw new InsufficientStockException("Cannot confirm more than reserved quantity");
        }

        this.reservedQuantity -= quantity;
        this.totalQuantity -= quantity;
        this.updatedAt = Instant.now();
    }

    public void addStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to add must be positive");
        }

        this.totalQuantity += quantity;
        this.availableQuantity += quantity;
        this.updatedAt = Instant.now();
    }

    public void releaseStock(int quantity) {
        if (quantity > reservedQuantity) {
            throw new IllegalArgumentException("Cannot release more than reserved quantity");
        }
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
        this.updatedAt = Instant.now();
    }

    // Domain exception
    public static class InsufficientStockException extends RuntimeException {
        public InsufficientStockException(String message) {
            super(message);
        }
    }
}
