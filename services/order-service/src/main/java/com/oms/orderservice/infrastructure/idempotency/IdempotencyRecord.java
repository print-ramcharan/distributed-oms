package com.oms.orderservice.infrastructure.idempotency;

import com.oms.orderservice.domain.idempotency.IdempotencyStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class IdempotencyRecord implements Serializable {

    private IdempotencyStatus status;
    private UUID orderId;
    private String response;
    private Instant createdAt;

}

