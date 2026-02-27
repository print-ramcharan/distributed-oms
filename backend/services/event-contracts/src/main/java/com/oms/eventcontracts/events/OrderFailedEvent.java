package com.oms.eventcontracts.events;

import java.util.UUID;

public record OrderFailedEvent(UUID orderId, String reason) {}
