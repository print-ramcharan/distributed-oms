package com.oms.eventcontracts.events;

import java.time.Instant;

public class InventoryUnavailableEvent {

    private String orderId;
    private String reason; // e.g. "INSUFFICIENT_STOCK: P1001"
    private Instant timestamp;

    // 1. Default Constructor (Required for Jackson)
    public InventoryUnavailableEvent() {
    }

    // 2. Constructor for UseCase
    public InventoryUnavailableEvent(String orderId, String reason, Instant timestamp) {
        this.orderId = orderId;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    // 3. Getters & Setters
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