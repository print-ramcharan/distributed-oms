package com.oms.orderservice.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.oms.orderservice.api.dto.CreateOrderRequest;
import com.oms.orderservice.api.dto.CreateOrderResponse;
import com.oms.orderservice.api.dto.OrderItemRequest;
import com.oms.orderservice.api.dto.OrderSummaryResponse;
import com.oms.orderservice.application.OrderCommandService;
import com.oms.orderservice.application.OrderQueryService;
import com.oms.orderservice.domain.model.Order;
import com.oms.orderservice.domain.model.OrderItem;
import com.oms.orderservice.domain.repository.OrderQueryRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.stream.Collectors;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.orderservice.infrastructure.idempotency.IdempotencyResult;
import com.oms.orderservice.infrastructure.idempotency.IdempotencyService;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderCommandService orderCommandService;
    private final OrderQueryService orderQueryService;
    private final OrderQueryRepository orderQueryRepository;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;


    public OrderController(OrderCommandService orderCommandService, OrderQueryService orderQueryService, OrderQueryRepository orderQueryRepository,
                           IdempotencyService idempotencyService, ObjectMapper objectMapper) {
        this.orderCommandService = orderCommandService;
        this.orderQueryService = orderQueryService;
        this.orderQueryRepository = orderQueryRepository;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreateOrderResponse> getOrder(@PathVariable UUID id) {
        return orderQueryService.findOrderById(id)
                .map(order ->
                        ResponseEntity.ok(
                                new CreateOrderResponse(order.getId(), order.getStatus())
                        )
                )
                .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping
    public ResponseEntity<List<OrderSummaryResponse>> listOrders(
            @RequestParam(defaultValue = "100") int limit
    ) {
        return ResponseEntity.ok(orderQueryRepository.findRecent(limit));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateOrderResponse createOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Idempotency-Key header is required");
        }

        IdempotencyResult result = idempotencyService.tryAcquire(idempotencyKey);

        if (!result.isAcquired()) {

            if (result.isCompleted()) {
                try {
                    return objectMapper.readValue(
                            result.getExistingRecord().getResponse(),
                            CreateOrderResponse.class);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to deserialize cached response", e);
                }
            }

            if (result.isInProgress()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Order creation already in progress for this Idempotency-Key");
            }
        }

        try {
            List<OrderItem> items = request.getItems()
                    .stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());

            Order order = orderCommandService.createOrder(items);

            CreateOrderResponse response = new CreateOrderResponse(order.getId(), order.getStatus());

            try {
                String responseJson = objectMapper.writeValueAsString(response);

                idempotencyService.markCompleted(
                        idempotencyKey,
                        order.getId(),
                        responseJson);
            } catch (JsonProcessingException e) {
                idempotencyService.clear(idempotencyKey);
                throw new IllegalStateException("Failed to serialize idempotent response", e);
            }

            return response;

        } catch (Exception ex) {
            idempotencyService.clear(idempotencyKey);
            throw ex;
        }

    }

    private OrderItem toDomain(OrderItemRequest item) {
        return OrderItem.create(item.getProductId(), item.getQuantity(), item.getPrice());

    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
