package com.oms.eventcontracts.events;

import java.time.Instant;
import java.util.UUID;

public class OrderCompletedEvent {

    private UUID orderId;
    private Instant occurredAt;

    // Required for Kafka / Jackson
    public OrderCompletedEvent() {
    }

    public OrderCompletedEvent(UUID orderId, Instant occurredAt) {
        this.orderId = orderId;
        this.occurredAt = occurredAt;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
