package com.oms.orderservice.application;

import com.oms.orderservice.domain.model.Order;
import com.oms.orderservice.domain.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderQueryService
 * Tests order retrieval and caching behavior
 */
@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderQueryService orderQueryService;

    @Test
    void shouldReturnOrderWhenFound() {
        // Given
        UUID orderId = UUID.randomUUID();
        Order order = mock(Order.class);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        Order result = orderQueryService.getOrderById(orderId);

        // Then
        assertThat(result).isEqualTo(order);
        verify(orderRepository).findById(orderId);
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFound() {
        // Given
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> orderQueryService.getOrderById(orderId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void shouldReturnOptionalWhenUsingFindMethod() {
        // Given
        UUID orderId = UUID.randomUUID();
        Order order = mock(Order.class);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        Optional<Order> result = orderQueryService.findOrderById(orderId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(order);
    }

    @Test
    void shouldReturnEmptyOptionalWhenOrderNotFoundUsingFindMethod() {
        // Given
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When
        Optional<Order> result = orderQueryService.findOrderById(orderId);

        // Then
        assertThat(result).isEmpty();
    }
}
