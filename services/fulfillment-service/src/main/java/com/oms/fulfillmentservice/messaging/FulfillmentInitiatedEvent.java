package com.oms.fulfillmentservice.messaging;

import java.time.Instant;

/**
 * Event published to fulfillment.initiated topic after a
 * FulfillmentTask is created and dispatched.
 */
public class FulfillmentInitiatedEvent {

    private String orderId;
    private String fulfillmentTaskId;
    private Instant initiatedAt;

    public FulfillmentInitiatedEvent() {
    }

    public FulfillmentInitiatedEvent(String orderId, String fulfillmentTaskId, Instant initiatedAt) {
        this.orderId = orderId;
        this.fulfillmentTaskId = fulfillmentTaskId;
        this.initiatedAt = initiatedAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getFulfillmentTaskId() {
        return fulfillmentTaskId;
    }

    public Instant getInitiatedAt() {
        return initiatedAt;
    }
}
