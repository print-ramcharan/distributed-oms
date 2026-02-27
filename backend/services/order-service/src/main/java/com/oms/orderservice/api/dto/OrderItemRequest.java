package com.oms.orderservice.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemRequest {

    @NotBlank(message = "product id is required")
    private String productId;

    @Min(value = 1, message = "quantity must be at least 1")
    private int quantity;

    @Positive
    private BigDecimal price;


}
