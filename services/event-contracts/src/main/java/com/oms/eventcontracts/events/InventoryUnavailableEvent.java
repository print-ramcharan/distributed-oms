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

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
