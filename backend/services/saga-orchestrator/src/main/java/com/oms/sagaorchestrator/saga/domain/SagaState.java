package com.oms.sagaorchestrator.saga.domain;

public enum SagaState {

    STARTED,

    PAYMENT_REQUESTED,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,

    INVENTORY_REQUESTED,
    INVENTORY_RESERVED,
    INVENTORY_FAILED,

    COMPENSATING,
    COMPENSATED,

    COMPLETED,
    FAILED
}
