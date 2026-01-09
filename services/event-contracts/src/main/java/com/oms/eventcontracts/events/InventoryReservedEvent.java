package com.oms.eventcontracts.events;

import java.time.Instant;

public class InventoryReservedEvent {

    private String orderId;
    private Instant timestamp;

    // 1. Default Constructor (Required for Jackson)
    public InventoryReservedEvent() {
    }

    // 2. Constructor for the UseCase
    public InventoryReservedEvent(String orderId, Instant timestamp) {
        this.orderId = orderId;
        this.timestamp = timestamp;
    }

    // 3. Getters & Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}