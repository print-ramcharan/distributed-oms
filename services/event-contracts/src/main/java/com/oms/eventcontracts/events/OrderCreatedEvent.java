package com.oms.eventcontracts.events;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderCreatedEvent {

    private UUID orderId;
    private BigDecimal amount;

    // Required by Jackson
    public OrderCreatedEvent() {
    }

    public OrderCreatedEvent(UUID orderId, BigDecimal amount) {
        this.orderId = orderId;
        this.amount = amount;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
