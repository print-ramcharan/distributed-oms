package com.oms.eventcontracts.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentCompletedEvent {
    private UUID orderId;
    private UUID paymentId;
    private BigDecimal amount;
    private Instant completedAt;

    public PaymentCompletedEvent() {}

    public PaymentCompletedEvent(UUID orderId, UUID paymentId, BigDecimal amount, Instant completedAt) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.completedAt = completedAt;
    }

    public UUID getOrderId() { return orderId; }
    public UUID getPaymentId() { return paymentId; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCompletedAt() { return completedAt; }
}
