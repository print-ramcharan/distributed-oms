package com.oms.fulfillmentservice.domain;

public enum FulfillmentStatus {
    INITIATED, // inventory reserved, task created
    DISPATCHED, // fulfillment.initiated event published
    FAILED // something went wrong
}
