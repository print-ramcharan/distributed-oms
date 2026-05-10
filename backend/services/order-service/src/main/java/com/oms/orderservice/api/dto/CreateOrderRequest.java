package com.oms.orderservice.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotEmpty(message = "items must not be empty")
    @Valid
    private List<OrderItemRequest> items;

    @NotEmpty(message = "customerEmail must not be empty")
    private String customerEmail;

    private UUID userId;

}
