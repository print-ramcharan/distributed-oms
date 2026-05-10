package com.oms.orderservice.api.dto;

import com.oms.orderservice.domain.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderResponse {

    private UUID orderId;
    private OrderStatus status;


}
