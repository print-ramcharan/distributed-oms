package com.oms.eventcontracts.events;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class OrderCreatedEvent extends BaseEvent {

    private UUID orderId;
    private String customerEmail;
    private BigDecimal amount;
    private List<OrderItemDTO> items;

    
    private String currency = "USD";
    private BigDecimal discount = BigDecimal.ZERO;

    
    public OrderCreatedEvent() {
        super(1);
    }

    
    public OrderCreatedEvent(UUID orderId, String customerEmail, BigDecimal amount, List<OrderItemDTO> items) {
        super(1);
        this.orderId = orderId;
        this.customerEmail = customerEmail;
        this.amount = amount;
        this.items = items;
    }

    
    public OrderCreatedEvent(UUID orderId, String customerEmail, BigDecimal amount, List<OrderItemDTO> items,
            String currency, BigDecimal discount) {
        super(2);
        this.orderId = orderId;
        this.customerEmail = customerEmail;
        this.amount = amount;
        this.items = items;
        this.currency = currency;
        this.discount = discount;
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

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getDiscount() {
        return discount;
    }
}
