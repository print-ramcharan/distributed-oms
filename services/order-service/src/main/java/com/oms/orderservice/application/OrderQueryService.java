package com.oms.orderservice.application;

import com.oms.orderservice.domain.model.Order;
import com.oms.orderservice.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;

    @Cacheable(value = "orders", key = "#id")
    public Order getOrderById(UUID id){
        return orderRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }
    public Optional<Order> findOrderById(UUID id) {
        return orderRepository.findById(id);
    }

}
