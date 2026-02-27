package com.oms.eventcontracts.events;

import java.time.Instant;

public class InventoryReserveRequestedEvent {

    private String orderId;
    private String productId;
    private int quantity;
    private Instant requestedAt;

    
    public InventoryReserveRequestedEvent() {}

    public InventoryReserveRequestedEvent(
            String orderId,
            String productId,
            int quantity,
            Instant requestedAt
    ) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.requestedAt = requestedAt;
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

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }
}
