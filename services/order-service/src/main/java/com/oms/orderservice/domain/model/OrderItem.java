package com.oms.orderservice.domain.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Objects;

@Data
public class OrderItem {

    private final String productId;

    private final int quantity;

    private final BigDecimal price;

    public OrderItem(String productId, int quantity, BigDecimal price){

        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
        if(price == null || price.signum() <= 0){
            throw new IllegalArgumentException("price must be positive");
        }

        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    public BigDecimal totalPrice(){
        return price.multiply(BigDecimal.valueOf(quantity));
    }
    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof OrderItem that)) return false;
        return quantity == that.quantity && productId.equals(that.productId) && price.equals(that.price);
    }

    @Override
    public int hashCode(){
        return Objects.hash(productId, quantity, price);
    }

}
