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


@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderQueryService orderQueryService;

    @Test
    void shouldReturnOrderWhenFound() {
        
        UUID orderId = UUID.randomUUID();
        Order order = mock(Order.class);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        
        Order result = orderQueryService.getOrderById(orderId);

        
        assertThat(result).isEqualTo(order);
        verify(orderRepository).findById(orderId);
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFound() {
        
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> orderQueryService.getOrderById(orderId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void shouldReturnOptionalWhenUsingFindMethod() {
        
        UUID orderId = UUID.randomUUID();
        Order order = mock(Order.class);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        
        Optional<Order> result = orderQueryService.findOrderById(orderId);

        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(order);
    }

    @Test
    void shouldReturnEmptyOptionalWhenOrderNotFoundUsingFindMethod() {
        
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        
        Optional<Order> result = orderQueryService.findOrderById(orderId);

        
        assertThat(result).isEmpty();
    }
}
