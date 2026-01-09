package com.oms.sagaorchestrator.saga.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "order_sagas")
@NoArgsConstructor
@Getter
public class OrderSaga {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID orderId;

    @OneToMany(mappedBy = "saga", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderSagaItem> items = new ArrayList<>();

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaState state;


    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    public OrderSaga(UUID orderId, BigDecimal amount) {
        this.orderId = orderId;
        this.amount = amount;
        this.state = SagaState.STARTED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void addItem(String productId, int quantity, BigDecimal price) {
        OrderSagaItem item = new OrderSagaItem(productId, quantity, price, this);
        this.items.add(item);
    }


    /* ===================== PAYMENT ===================== */

    public void markPaymentRequested() {
        assertState(SagaState.STARTED);
        transitionTo(SagaState.PAYMENT_REQUESTED);
    }

    public void markPaymentCompleted() {
        assertState(SagaState.PAYMENT_REQUESTED);
        transitionTo(SagaState.PAYMENT_COMPLETED);
    }

    public void markPaymentFailed() {
        assertState(SagaState.PAYMENT_REQUESTED);
        transitionTo(SagaState.PAYMENT_FAILED);
    }

    /* ===================== INVENTORY ===================== */

    public void markInventoryRequested() {
        assertState(SagaState.PAYMENT_COMPLETED);
        transitionTo(SagaState.INVENTORY_REQUESTED);
    }

    public void markInventoryReserved() {
        assertState(SagaState.INVENTORY_REQUESTED);
        transitionTo(SagaState.INVENTORY_RESERVED);
    }

    public void markInventoryFailed() {
        assertState(SagaState.INVENTORY_REQUESTED);
        transitionTo(SagaState.INVENTORY_FAILED);
    }

    /* ===================== COMPENSATION ===================== */

    public void markCompensating() {
        if (state != SagaState.INVENTORY_FAILED &&
                state != SagaState.PAYMENT_COMPLETED) {
            throw new IllegalStateException(
                    "Cannot compensate from state " + state
            );
        }
        transitionTo(SagaState.COMPENSATING);
    }

    public void markCompensated() {
        assertState(SagaState.COMPENSATING);
        transitionTo(SagaState.COMPENSATED);
    }

    /* ===================== TERMINAL ===================== */

    public void markCompleted() {
        assertState(SagaState.INVENTORY_RESERVED);
        transitionTo(SagaState.COMPLETED);
    }

    public void markFailed() {
        if (state != SagaState.PAYMENT_FAILED &&
                state != SagaState.COMPENSATED) {
            throw new IllegalStateException(
                    "Cannot fail saga from state " + state
            );
        }
        transitionTo(SagaState.FAILED);
    }

    /* ===================== INTERNAL ===================== */

    private void assertState(SagaState expected) {
        if (this.state != expected) {
            throw new IllegalStateException(
                    "Invalid saga transition from " + this.state +
                            ", expected " + expected
            );
        }
    }

    private void transitionTo(SagaState newState) {
        this.state = newState;
        this.updatedAt = Instant.now();
    }

    public boolean allItemsReserved() {
        return items != null &&
                !items.isEmpty() &&
                items.stream().allMatch(OrderSagaItem::isReserved);
    }

    public boolean isItemReserved(String productId) {
        return items.stream()
                .anyMatch(i ->
                        i.getProductId().equals(productId) && i.isReserved()
                );
    }

    public void markItemReserved(String productId) {
        this.items.stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .ifPresent(OrderSagaItem::markReserved);
    }



}
