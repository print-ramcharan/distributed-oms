package com.oms.sagaorchestrator.saga.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_sagas")
@NoArgsConstructor
@Getter
public class OrderSaga {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaState state;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    public OrderSaga(UUID orderId) {
        this.orderId = orderId;
        this.state = SagaState.STARTED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /* ===================== STATE TRANSITIONS ===================== */

    public void markPaymentInitiated() {
        assertState(SagaState.STARTED);
        this.state = SagaState.PAYMENT_INITIATED;
        touch();
    }

    public void markPaymentCompleted() {
        assertState(SagaState.PAYMENT_INITIATED);
        this.state = SagaState.PAYMENT_COMPLETED;
        touch();
    }

    public void markPaymentFailed() {
        assertState(SagaState.PAYMENT_INITIATED);
        this.state = SagaState.PAYMENT_FAILED;
        touch();
    }

    public void markOrderCompleted() {
        assertState(SagaState.PAYMENT_COMPLETED);
        this.state = SagaState.ORDER_COMPLETED;
        touch();
    }

    public void markOrderCancelled() {
        assertState(SagaState.PAYMENT_FAILED);
        this.state = SagaState.ORDER_CANCELLED;
        touch();
    }

    /* ===================== INTERNAL ===================== */

    private void assertState(SagaState expected) {
        if (this.state != expected) {
            throw new IllegalStateException(
                    "Invalid saga transition from " + this.state + " expected " + expected
            );
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
