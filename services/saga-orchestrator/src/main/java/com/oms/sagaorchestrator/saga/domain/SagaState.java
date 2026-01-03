package com.oms.sagaorchestrator.saga.domain;

public enum SagaState {

    // Saga record created, nothing triggered yet
    STARTED,

    // Payment command sent to Payment Service
    PAYMENT_INITIATED,

    // Payment service confirmed success
    PAYMENT_COMPLETED,

    // Payment service reported failure
    PAYMENT_FAILED,

    // Order service confirmed order completion
    ORDER_COMPLETED,

    // Order service rolled back / cancelled
    ORDER_CANCELLED
}
