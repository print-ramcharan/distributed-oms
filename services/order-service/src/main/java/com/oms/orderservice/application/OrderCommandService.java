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
import java.util.UUID;
import java.util.stream.Collectors;

import static com.oms.orderservice.domain.outbox.AggregateType.ORDER;

@Service
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;
    private final io.micrometer.core.instrument.Counter ordersCreatedCounter;
    private final io.micrometer.core.instrument.DistributionSummary revenueSummary;

    public OrderCommandService(
            OrderRepository orderRepository,
            OutboxRepository outboxRepository,
            ObjectMapper objectMapper,
            io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;

        this.ordersCreatedCounter = io.micrometer.core.instrument.Counter.builder("omniorder_orders_created_total")
                .description("Total number of orders created")
                .register(meterRegistry);

        this.revenueSummary = io.micrometer.core.instrument.DistributionSummary.builder("omniorder_revenue_total")
                .description("Total revenue from orders")
                .register(meterRegistry);
    }

    @org.springframework.transaction.annotation.Transactional
    public Order createOrder(List<OrderItem> items, String customerEmail, UUID userId) {

        Order order = Order.create(items, customerEmail, userId);
        orderRepository.save(order);

        ordersCreatedCounter.increment();
        revenueSummary.record(order.getTotalAmount().doubleValue());

        List<OrderItemDTO> itemDtos = order.getItems()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                order.getCustomerEmail(),
                order.getTotalAmount(),
                itemDtos,
                "USD",
                java.math.BigDecimal.ZERO);

        OutboxEvent outboxEvent = OutboxEvent.create(
                order.getId(),
                ORDER,
                OrderCreatedEvent.class.getSimpleName(),
                serialize(event));

        outboxRepository.save(outboxEvent);

        return order;
    }

    private OrderItemDTO toDto(OrderItem item) {
        return new OrderItemDTO(
                item.getProductId(),
                item.getQuantity(),
                item.getPrice());
    }

    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize event", e);
        }
    }
}
