package com.oms.orderservice.domain.model;

import lombok.Data;

import java.util.Objects;

@Data
public class OrderItem {

    private final String productId;

    private final int quantity;

    public OrderItem(String productId, int quantity){

        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }

        this.productId = productId;
        this.quantity = quantity;
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof OrderItem that)) return false;
        return quantity == that.quantity && productId.equals(that.productId);
    }

    @Override
    public int hashCode(){
        return Objects.hash(productId, quantity);
    }

}
