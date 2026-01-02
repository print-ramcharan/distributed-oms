package com.oms.paymentservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class OrderCreatedEvent {

    private UUID orderId;
    private BigDecimal amount;

}
