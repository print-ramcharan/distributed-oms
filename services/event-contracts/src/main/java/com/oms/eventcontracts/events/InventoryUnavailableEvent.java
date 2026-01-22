package com.oms.eventcontracts.events;

import java.time.Instant;

public class InventoryUnavailableEvent {

    private String orderId;
    private String reason;
    private Instant timestamp;

    // ✅ Required by Jackson
    public InventoryUnavailableEvent() {
    }

    // ✅ Producer-side constructor
    public InventoryUnavailableEvent(String orderId, String reason, Instant timestamp) {
        this.orderId = orderId;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getReason() {
        return reason;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
