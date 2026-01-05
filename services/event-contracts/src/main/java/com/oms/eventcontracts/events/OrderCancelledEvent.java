package com.oms.eventcontracts.events;

import java.time.Instant;
import java.util.UUID;

public class OrderCancelledEvent {

    private UUID orderId;
    private String reason;
    private Instant occurredAt;

    // Required for Kafka / Jackson
    public OrderCancelledEvent() {
    }

    public OrderCancelledEvent(UUID orderId, String reason, Instant occurredAt) {
        this.orderId = orderId;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getReason() {
        return reason;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
