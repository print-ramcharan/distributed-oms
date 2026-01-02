package com.oms.orderservice.api;

import com.oms.orderservice.api.dto.CreateOrderRequest;
import com.oms.orderservice.api.dto.CreateOrderResponse;
import com.oms.orderservice.api.dto.OrderItemRequest;
import com.oms.orderservice.application.OrderCommandService;
import com.oms.orderservice.domain.model.Order;
import com.oms.orderservice.domain.model.OrderItem;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderCommandService orderCommandService;

    public OrderController(OrderCommandService orderCommandService){
        this.orderCommandService = orderCommandService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateOrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request){
        List<OrderItem> items = request.getItems().stream().map(this::toDomain).collect(Collectors.toList());

        Order order = orderCommandService.createOrder(items);

        return new CreateOrderResponse(order.getId(), order.getStatus());
    }

    private OrderItem toDomain(OrderItemRequest item){
        return new OrderItem(item.getProductId(), item.getQuantity(), item.getPrice());

    }
    @GetMapping("/health")
    public String health(){
        return "OK";
    }
}
