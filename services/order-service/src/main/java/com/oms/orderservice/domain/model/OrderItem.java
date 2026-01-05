package com.oms.orderservice.domain.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal price;

    protected OrderItem(){}

    private OrderItem(String productId, int quantity, BigDecimal price){
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("price must be positive");
        }
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    public static OrderItem create(String productId, int quantity, BigDecimal price){
        return new OrderItem(productId, quantity, price);
    }

    public BigDecimal totalPrice(){
        return price.multiply(BigDecimal.valueOf(quantity));
    }


}
