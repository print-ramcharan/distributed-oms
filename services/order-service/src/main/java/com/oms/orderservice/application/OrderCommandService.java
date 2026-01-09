package com.oms.orderservice.application;

import com.oms.eventcontracts.events.OrderCreatedEvent;
import com.oms.eventcontracts.events.OrderItemDTO;
import com.oms.orderservice.domain.model.Order;
import com.oms.orderservice.domain.model.OrderItem;
import com.oms.orderservice.domain.outbox.OutboxEvent;
import com.oms.orderservice.domain.outbox.OutboxRepository;
import com.oms.orderservice.domain.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.oms.orderservice.domain.outbox.AggregateType.ORDER;

@Service
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderCommandService(
            OrderRepository orderRepository,
            OutboxRepository outboxRepository,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public Order createOrder(List<OrderItem> items) {
        Order order = Order.create(items);
        orderRepository.save(order);

        // Convert domain objects → DTOs
        List<OrderItemDTO> itemDtos = order.getItems()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        OrderCreatedEvent event =
                new OrderCreatedEvent(
                        order.getId(),
                        order.getTotalAmount(),
                        itemDtos
                );

        OutboxEvent outboxEvent = OutboxEvent.create(
                order.getId(),
                ORDER,
                OrderCreatedEvent.class.getSimpleName(),
                serialize(event)
        );

        outboxRepository.save(outboxEvent);

        return order;
    }

    private OrderItemDTO toDto(OrderItem item) {
        return new OrderItemDTO(
                item.getProductId(),
                item.getQuantity(),
                item.getPrice()
        );
    }

    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize event", e);
        }
    }
}
