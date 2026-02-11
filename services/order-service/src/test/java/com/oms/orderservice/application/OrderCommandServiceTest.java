package com.oms.orderservice.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.orderservice.domain.model.Order;
import com.oms.orderservice.domain.model.OrderItem;
import com.oms.orderservice.domain.outbox.OutboxEvent;
import com.oms.orderservice.domain.outbox.OutboxRepository;
import com.oms.orderservice.domain.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCommandServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderCommandService orderCommandService;

    @Test
    void shouldCreateOrderAndOutboxEvent() throws Exception {
        // Given
        OrderItem item = OrderItem.create("prod-1", 1, BigDecimal.valueOf(100));
        List<OrderItem> items = List.of(item);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        Order result = orderCommandService.createOrder(items, "test@example.com");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        verify(orderRepository).save(any(Order.class));
        verify(outboxRepository).save(any(OutboxEvent.class));
    }

    @Test
    void shouldCalculateTotalAmountCorrectly() throws Exception {
        // Given
        OrderItem item1 = OrderItem.create("prod-1", 2, BigDecimal.valueOf(50.00));
        OrderItem item2 = OrderItem.create("prod-2", 1, BigDecimal.valueOf(100.00));
        List<OrderItem> items = List.of(item1, item2);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        Order result = orderCommandService.createOrder(items, "test@example.com");

        // Then
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(200.00));
    }

    @Test
    void shouldHandleMultipleItems() throws Exception {
        // Given
        List<OrderItem> items = List.of(
                OrderItem.create("prod-1", 2, BigDecimal.valueOf(50)),
                OrderItem.create("prod-2", 1, BigDecimal.valueOf(100)),
                OrderItem.create("prod-3", 3, BigDecimal.valueOf(30)));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        Order result = orderCommandService.createOrder(items, "test@example.com");

        // Then
        assertThat(result.getItems()).hasSize(3);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(290.00));
    }

    @Test
    void shouldStoreCustomerEmail() throws Exception {
        // Given
        OrderItem item = OrderItem.create("prod-1", 1, BigDecimal.valueOf(100));
        String email = "customer@test.com";

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        Order result = orderCommandService.createOrder(List.of(item), email);

        // Then
        assertThat(result.getCustomerEmail()).isEqualTo(email);
    }

    @Test
    void shouldThrowExceptionWhenSerializationFails() throws Exception {
        // Given
        OrderItem item = OrderItem.create("prod-1", 1, BigDecimal.valueOf(100));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization failed"));

        // When/Then
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> {
            orderCommandService.createOrder(List.of(item), "test@example.com");
        });
    }
}
