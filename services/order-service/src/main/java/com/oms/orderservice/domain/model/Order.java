package com.oms.orderservice.domain.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class Order {
    private final UUID id;
    private final List<OrderItem> items;
    private final BigDecimal totalAmount;
    private OrderStatus status;
    private final Instant createdAt;

    public Order(UUID id, List<OrderItem> items){
        if(items == null || items.isEmpty()){
            throw new IllegalArgumentException("Order must contain atleast one item");
        }

        this.id = id;
        this.items = items;
        this.totalAmount = calculateTotal(items);
        this.status = OrderStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public static Order create(List<OrderItem> items) {
        return new Order(UUID.randomUUID(), items);
    }

    private BigDecimal calculateTotal(List<OrderItem> items){
        return items.stream().map(OrderItem::totalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
