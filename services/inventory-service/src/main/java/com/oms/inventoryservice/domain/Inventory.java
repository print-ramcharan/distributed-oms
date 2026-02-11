package com.oms.inventoryservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inventory")
@NoArgsConstructor
@Getter
public class Inventory {

    @Id
    @Column(nullable = false, updatable = false)
    private String productId;

    @Column(nullable = false)
    private int availableQuantity;

    @Column(nullable = false)
    private int reservedQuantity;

    public Inventory(String productId, int availableQuantity, int reservedQuantity) {
        this.productId = productId;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
    }

    public boolean hasAvailableStock(int quantity) {
        return this.availableQuantity >= quantity;
    }

    public void reserve(int quantity) {
        if (!hasAvailableStock(quantity)) {
            throw new IllegalStateException("Insufficient stock for product: " + productId);
        }
        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
    }

    public void release(int quantity) {
        if (this.reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot release more than reserved quantity");
        }
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
    }

    public void confirm(int quantity) {
        if (this.reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot confirm more than reserved quantity");
        }
        this.reservedQuantity -= quantity;
    }
}
