package com.oms.eventcontracts.events;

import com.oms.eventcontracts.enums.OrderProgress;
import java.util.UUID;

public class OrderProgressUpdatedEvent {

    private UUID orderId;
    private OrderProgress progress;

    public OrderProgressUpdatedEvent() {
    }

    public OrderProgressUpdatedEvent(UUID orderId, OrderProgress progress) {
        this.orderId = orderId;
        this.progress = progress;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public OrderProgress getProgress() {
        return progress;
    }
}
