package com.oms.eventcontracts.events;

import java.time.Instant;

public class InventoryUnavailableEvent {

    private String orderId;
    private String productId;
    private int requestedQuantity;
    private String reason;
    private Instant timestamp;

    // Required no-arg constructor
    public InventoryUnavailableEvent() {
    }

    public InventoryUnavailableEvent(
            String orderId,
            String productId,
            int requestedQuantity,
            String reason,
            Instant timestamp
    ) {
        this.orderId = orderId;
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.reason = reason;
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

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(int requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
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
