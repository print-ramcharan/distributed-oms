package com.oms.orderservice.domain.event;

import com.oms.events.OrderCreatedEvent;

public interface OrderEventPublisher {

    void publish(OrderCreatedEvent event);
}
