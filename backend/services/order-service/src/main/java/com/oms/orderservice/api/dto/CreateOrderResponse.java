package com.oms.orderservice.api.dto;

import com.oms.orderservice.domain.model.OrderStatus;
import jdk.jshell.Snippet;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateOrderResponse {

    private UUID orderId;
    private OrderStatus status;

    public CreateOrderResponse(UUID orderId, OrderStatus status){
        this.orderId = orderId;
        this.status = status;
    }

}
