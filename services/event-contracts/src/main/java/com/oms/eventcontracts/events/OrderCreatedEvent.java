package com.oms.eventcontracts.events;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class OrderCreatedEvent {

    private UUID orderId;
    private String customerEmail;
    private BigDecimal amount;
    private List<OrderItemDTO> items;

    // Jackson
    public OrderCreatedEvent() {
    }

    public OrderCreatedEvent(UUID orderId, String customerEmail, BigDecimal amount, List<OrderItemDTO> items) {
        this.orderId = orderId;
        this.customerEmail = customerEmail;
        this.amount = amount;
        this.items = items;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public List<OrderItemDTO> getItems() {
        return items;
    }
}
