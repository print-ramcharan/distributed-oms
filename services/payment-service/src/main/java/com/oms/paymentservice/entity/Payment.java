package com.oms.paymentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payments", uniqueConstraints = @UniqueConstraint(columnNames = "order_id"))
public class Payment {
    @Id
    @Column(nullable = false)
    private UUID id;
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false)
    private String currency;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isRefunded() {
        return status == PaymentStatus.REFUNDED;
    }
    public void refund(BigDecimal refundAmount) {

        if (status == PaymentStatus.REFUNDED) {
            // Idempotent: Kafka redelivery safe
            return;
        }

        if (status != PaymentStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Cannot refund payment in status " + status
            );
        }

        if (refundAmount.compareTo(amount) > 0) {
            throw new IllegalArgumentException(
                    "Refund amount cannot exceed paid amount"
            );
        }

        // Full refund (for now)
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = Instant.now();
    }


}
