package com.oms.orderservice.infrastructure.messaging;

import com.oms.events.OrderCreatedEvent;
import com.oms.orderservice.domain.event.OrderEventPublisher;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;


@Component
public class LoggingOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingOrderEventPublisher.class);

    @Override
    public void publish(OrderCreatedEvent event){
        log.info("Publishing OrderCreatedEvent: orderId={}", event.getOrderId());
    }
}
