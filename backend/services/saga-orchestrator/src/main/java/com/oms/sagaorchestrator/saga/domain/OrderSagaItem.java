package com.oms.sagaorchestrator.saga.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_saga_items")
@Getter
@NoArgsConstructor
public class OrderSagaItem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private boolean reserved = false;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderSaga saga;

    public OrderSagaItem(
            String productId,
            int quantity,
            BigDecimal price,
            OrderSaga saga
    ) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.saga = saga;
    }

    public void markReserved() {
        this.reserved = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderSagaItem)) return false;
        return id != null && id.equals(((OrderSagaItem) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
