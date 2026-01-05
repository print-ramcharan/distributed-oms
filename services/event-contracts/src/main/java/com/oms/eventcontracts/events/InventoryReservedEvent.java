package com.oms.eventcontracts.events;

import java.time.Instant;

public class InventoryReservedEvent {

    private String orderId;
    private String productId;
    private int quantity;
    private Instant timestamp;

    // Required no-arg constructor for Jackson
    public InventoryReservedEvent() {
    }

    public InventoryReservedEvent(
            String orderId,
            String productId,
            int quantity,
            Instant timestamp
    ) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.timestamp = timestamp;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
