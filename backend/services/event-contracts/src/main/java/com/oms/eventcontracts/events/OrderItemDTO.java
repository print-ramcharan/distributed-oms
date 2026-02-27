package com.oms.eventcontracts.events;

import java.math.BigDecimal;

public class OrderItemDTO {

    private String productId;
    private int quantity;
    private BigDecimal price;

    public OrderItemDTO() {
    }

    public OrderItemDTO(String productId, int quantity, BigDecimal price) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
