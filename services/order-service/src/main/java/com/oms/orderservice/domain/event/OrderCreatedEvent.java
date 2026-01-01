package com.oms.orderservice.domain.event;

import java.util.UUID;

public record OrderCreatedEvent(UUID orderId){ }
