package com.oms.orderservice.domain.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.io.Serializable;

@Entity
@Table(name = "order_items")
@Getter
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OrderItem implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_item_id", updatable = false, nullable = false)
    private UUID id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal price;

    protected OrderItem() {
        
    }

    private OrderItem(String productId, int quantity, BigDecimal price) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be > 0");
        }
        if (price.signum() <= 0) {
            throw new IllegalArgumentException("Price must be > 0");
        }

        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    

    public static OrderItem create(String productId, int quantity, BigDecimal price) {
        return new OrderItem(productId, quantity, price);
    }

    

    void attachTo(Order order) {
        this.order = order;
    }

    

    public BigDecimal totalPrice() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
