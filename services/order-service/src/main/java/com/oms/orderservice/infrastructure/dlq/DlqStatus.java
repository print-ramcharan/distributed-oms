package com.oms.orderservice.infrastructure.dlq;

public enum DlqStatus {
    PENDING, 
    RETRIED, 
    RESOLVED 
}
