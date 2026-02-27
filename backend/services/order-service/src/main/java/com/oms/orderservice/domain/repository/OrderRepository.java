package com.oms.orderservice.domain.repository;

import com.oms.orderservice.domain.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Override
    Optional<Order> findById(UUID orderId);
}
