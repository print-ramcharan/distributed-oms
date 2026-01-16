package com.oms.eventcontracts.events;

import java.util.UUID;

public record OrderCompletedEvent(UUID orderId, String completedAt) {}
