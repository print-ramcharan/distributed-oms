package com.oms.eventcontracts.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentRefundedEvent {

    private UUID orderId;
    private UUID paymentId;
    private BigDecimal amount;
    private String reason;
    private Instant refundedAt;

    public PaymentRefundedEvent() {
    }

    public PaymentRefundedEvent(
            UUID orderId,
            UUID paymentId,
            BigDecimal amount,
            String reason,
            Instant refundedAt
    ) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.reason = reason;
        this.refundedAt = refundedAt;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public Instant getRefundedAt() {
        return refundedAt;
    }
}
