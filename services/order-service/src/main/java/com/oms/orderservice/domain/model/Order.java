package com.oms.orderservice.domain.model;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class Order {
    private final UUID id;
    private OrderStatus status;
    private final Instant createdAt;

    public Order(UUID id, List<OrderItem> items){
        this.id = id;
        this.status = OrderStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public static Order create(List<OrderItem> items) {
        return new Order(UUID.randomUUID(), items);
    }



}
