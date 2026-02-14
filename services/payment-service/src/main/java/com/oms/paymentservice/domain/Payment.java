package com.oms.paymentservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@NoArgsConstructor
@Getter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(nullable = false)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Payment(UUID orderId, BigDecimal amount, String transactionId, String idempotencyKey, String paymentMethod) {
        this.orderId = orderId;
        this.amount = amount;
        this.transactionId = transactionId;
        this.idempotencyKey = idempotencyKey;
        this.paymentMethod = paymentMethod;
        this.status = PaymentStatus.PENDING;
        this.currency = "USD"; // Default currency
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // Constructor for backward compatibility (defaults new fields to
    // null/generated)
    public Payment(UUID orderId, BigDecimal amount) {
        this(orderId, amount, UUID.randomUUID().toString(), null, "UNKNOWN");
    }

    public void markCompleted() {
        assertStatus(PaymentStatus.PENDING, "Cannot complete payment from status: " + status);
        this.status = PaymentStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public void markRefunded() {
        assertStatus(PaymentStatus.COMPLETED, "Cannot refund payment from status: " + status);
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        if (status != PaymentStatus.PENDING && status != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Cannot fail payment from status: " + status);
        }
        this.status = PaymentStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    private void assertStatus(PaymentStatus expected, String errorMessage) {
        if (this.status != expected) {
            throw new IllegalStateException(errorMessage);
        }
    }

    // ========== Backward-compatible methods ==========

    public void refund(BigDecimal amount) {
        markRefunded();
    }

    public boolean isRefunded() {
        return this.status == PaymentStatus.REFUNDED;
    }
}
