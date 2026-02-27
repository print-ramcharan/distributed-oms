package com.oms.eventcontracts.events;

import java.time.Instant;
import java.util.UUID;

public class PaymentFailedEvent {
    private UUID orderId;
    private UUID paymentId;
    private String reason;
    private Instant failedAt;

    public PaymentFailedEvent() {}

    public PaymentFailedEvent(UUID orderId, UUID paymentId, String reason, Instant failedAt) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.reason = reason;
        this.failedAt = failedAt;
    }

    public UUID getOrderId() { return orderId; }
    public UUID getPaymentId() { return paymentId; }
    public String getReason() { return reason; }
    public Instant getFailedAt() { return failedAt; }
}
