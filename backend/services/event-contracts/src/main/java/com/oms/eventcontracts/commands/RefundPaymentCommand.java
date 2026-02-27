package com.oms.eventcontracts.commands;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class RefundPaymentCommand {

    private UUID orderId;
    private BigDecimal amount;
    private String reason;
    private Instant requestedAt;

    public RefundPaymentCommand() {
    }

    public RefundPaymentCommand(UUID orderId, BigDecimal amount, String reason, Instant requestedAt) {
        this.orderId = orderId;
        this.amount = amount;
        this.reason = reason;
        this.requestedAt = requestedAt;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }
}
