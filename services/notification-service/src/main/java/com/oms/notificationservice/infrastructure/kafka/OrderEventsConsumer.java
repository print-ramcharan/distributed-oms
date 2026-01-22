package com.oms.notificationservice.infrastructure.kafka;

import com.oms.eventcontracts.events.OrderCreatedEvent;
import com.oms.notificationservice.application.EmailService;
import com.oms.notificationservice.domain.Notification;
import com.oms.notificationservice.domain.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventsConsumer {

    private final EmailService emailService;
    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = "order.event.created", groupId = "notification-service-group")
    public void handleOrderEvents(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for Order ID: {}", event.getOrderId());

        if (event.getCustomerEmail() == null || event.getCustomerEmail().isEmpty()) {
            log.warn("No email provided for Order ID: {}. Skipping notification.", event.getOrderId());
            return;
        }

        // Send Email
        emailService.sendOrderConfirmation(
                event.getCustomerEmail(),
                event.getOrderId().toString(),
                event.getAmount().toString());

        // Audit Log
        Notification notification = new Notification(
                event.getOrderId(),
                event.getCustomerEmail(),
                "Order Confirmation",
                "ORDER_CREATED");
        notificationRepository.save(notification);
        log.info("Notification saved for Order ID: {}", event.getOrderId());
    }
}
