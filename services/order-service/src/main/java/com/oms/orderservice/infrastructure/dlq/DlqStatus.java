package com.oms.orderservice.infrastructure.dlq;

public enum DlqStatus {
    PENDING, // received, not yet actioned
    RETRIED, // retry was triggered via admin endpoint
    RESOLVED // manually marked resolved (e.g. acknowledged as expected failure)
}
