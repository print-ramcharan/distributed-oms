package com.oms.orderservice.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderItemRequest {

    @NotBlank(message = "product id is required")
    private String productId;

    @Min(value = 1, message = "quantity must be at least 1")
    private int quantity;


}
