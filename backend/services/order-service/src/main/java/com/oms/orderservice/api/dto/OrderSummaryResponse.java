package com.oms.orderservice.api.dto;

import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID orderId,
        String status,
        Instant createdAt
) {}
