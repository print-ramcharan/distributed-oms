package com.oms.orderservice.domain.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id")
    private UUID id;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Order() {}

    public Order(List<OrderItem> items){
        if(items == null || items.isEmpty()){
            throw new IllegalArgumentException("Order must contain atleast one item");
        }

        this.items = items;
        this.totalAmount = calculateTotal(items);
        this.status = OrderStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public static Order create(List<OrderItem> items) {
        return new Order(items);
    }

    private BigDecimal calculateTotal(List<OrderItem> items){
        return items.stream().map(OrderItem::totalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
