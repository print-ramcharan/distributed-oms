package com.oms.orderservice.application;

import com.oms.eventcontracts.events.OrderCreatedEvent;
import com.oms.orderservice.domain.model.Order;
import com.oms.orderservice.domain.model.OrderItem;
import com.oms.orderservice.domain.outbox.OutboxEvent;
import com.oms.orderservice.domain.outbox.OutboxRepository;
import com.oms.orderservice.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;
import static com.oms.orderservice.domain.outbox.AggregateType.ORDER;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
public class OrderCommandService {
    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    OrderCommandService(OrderRepository orderRepository, OutboxRepository outboxRepository, ObjectMapper objectMapper){
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }
    public Order createOrder(List<OrderItem> items){
        Order order = Order.create(items);
        orderRepository.save(order);

        OrderCreatedEvent event = new OrderCreatedEvent(order.getId(), order.getTotalAmount());

        OutboxEvent outboxEvent = OutboxEvent.create(
                order.getId(),
                ORDER,
                event.getClass().getSimpleName(),
                serialize(event)
        );

        outboxRepository.save(outboxEvent);

        return order;
    }
    private String serialize(Object event){
        try {
            return objectMapper.writeValueAsString(event);
        }catch (Exception e){
            throw new IllegalStateException("Failed to serialize event", e);
        }
    }
}
