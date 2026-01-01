package com.oms.orderservice.domain.event;

public interface OrderEventPublisher {

    void publish(OrderCreatedEvent event);
}
