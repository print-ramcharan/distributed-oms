package com.oms.eventcontracts.commands;

import com.oms.eventcontracts.enums.OrderProgress;
import java.util.UUID;

public class AdvanceOrderProgressCommand {

    private UUID orderId;
    private OrderProgress targetProgress;

    public AdvanceOrderProgressCommand() {}

    public AdvanceOrderProgressCommand(UUID orderId, OrderProgress targetProgress) {
        this.orderId = orderId;
        this.targetProgress = targetProgress;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public OrderProgress getTargetProgress() {
        return targetProgress;
    }
}
