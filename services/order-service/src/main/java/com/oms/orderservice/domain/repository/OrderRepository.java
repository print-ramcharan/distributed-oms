package com.oms.orderservice.domain.repository;

import com.oms.orderservice.domain.model.Order;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    void save(Order order);
    Optional<Order> findById(UUID orderId);
}
