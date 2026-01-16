package com.oms.orderservice.domain.model;

public enum OrderStatus {
    PENDING,     // Not final yet
    COMPLETED,   // Success (confirmed + done)
    CANCELLED,   // Failed / compensated
    FAILED       // (optional — you may even remove this later)
}



