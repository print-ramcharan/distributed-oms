package com.oms.orderservice.application;

import com.oms.eventcontracts.events.OrderCreatedEvent;
import com.oms.orderservice.domain.event.OrderEventPublisher;
import com.oms.orderservice.domain.model.Order;
import com.oms.orderservice.domain.model.OrderItem;
import com.oms.orderservice.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrderCommandService {
    private final OrderRepository orderRepository;
//    private final OrderEventPublisher orderEventPublisher;

    OrderCommandService(OrderRepository orderRepository, OrderEventPublisher orderEventPublisher){
        this.orderRepository = orderRepository;
//        this.orderEventPublisher = orderEventPublisher;
    }
    public Order createOrder(List<OrderItem> items){
        Order order = Order.create(items);
        orderRepository.save(order);
//        orderEventPublisher.publish(new OrderCreatedEvent(order.getId(), order.getTotalAmount()));
        return order;
    }
}
