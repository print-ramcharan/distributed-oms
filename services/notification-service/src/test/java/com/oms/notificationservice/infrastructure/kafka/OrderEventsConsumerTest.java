package com.oms.notificationservice.infrastructure.kafka;

import com.oms.eventcontracts.events.OrderCreatedEvent;
import com.oms.notificationservice.application.EmailService;
import com.oms.notificationservice.domain.Notification;
import com.oms.notificationservice.domain.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventsConsumer Unit Tests")
class OrderEventsConsumerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private OrderEventsConsumer orderEventsConsumer;

    @Test
    @DisplayName("Should process OrderCreatedEvent and send email")
    void shouldProcessOrderCreatedEventAndSendEmail() {
        
        UUID orderId = UUID.randomUUID();
        String customerEmail = "customer@example.com";
        BigDecimal amount = new BigDecimal("150.00");

        OrderCreatedEvent event = new OrderCreatedEvent(orderId, customerEmail, amount, null);

        
        orderEventsConsumer.handleOrderEvents(event);

        
        verify(emailService, times(1)).sendOrderConfirmation(
                customerEmail,
                orderId.toString(),
                amount.toString());
    }

    @Test
    @DisplayName("Should save notification to repository")
    void shouldSaveNotificationToRepository() {
        
        UUID orderId = UUID.randomUUID();
        String customerEmail = "customer@example.com";
        BigDecimal amount = new BigDecimal("200.00");

        OrderCreatedEvent event = new OrderCreatedEvent(orderId, customerEmail, amount, null);

        
        orderEventsConsumer.handleOrderEvents(event);

        
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getOrderId()).isEqualTo(orderId);
        assertThat(savedNotification.getRecipient()).isEqualTo(customerEmail);
        assertThat(savedNotification.getSubject()).isEqualTo("Order Confirmation");
        assertThat(savedNotification.getType()).isEqualTo("ORDER_CREATED");
        assertThat(savedNotification.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("Should skip notification when customer email is null")
    void shouldSkipNotificationWhenCustomerEmailIsNull() {
        
        UUID orderId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, null, new BigDecimal("100.00"), null);

        
        orderEventsConsumer.handleOrderEvents(event);

        
        verify(emailService, never()).sendOrderConfirmation(any(), any(), any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should skip notification when customer email is empty")
    void shouldSkipNotificationWhenCustomerEmailIsEmpty() {
        
        UUID orderId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, "", new BigDecimal("100.00"), null);

        
        orderEventsConsumer.handleOrderEvents(event);

        
        verify(emailService, never()).sendOrderConfirmation(any(), any(), any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle complete event processing flow")
    void shouldHandleCompleteEventProcessingFlow() {
        
        UUID orderId = UUID.randomUUID();
        String customerEmail = "john.doe@example.com";
        BigDecimal amount = new BigDecimal("999.99");

        OrderCreatedEvent event = new OrderCreatedEvent(orderId, customerEmail, amount, null);

        
        orderEventsConsumer.handleOrderEvents(event);

        
        
        verify(emailService).sendOrderConfirmation(
                customerEmail,
                orderId.toString(),
                amount.toString());

        
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getOrderId()).isEqualTo(orderId);
        assertThat(notification.getRecipient()).isEqualTo(customerEmail);
        assertThat(notification.getType()).isEqualTo("ORDER_CREATED");
    }

    @Test
    @DisplayName("Should process event with large order amount")
    void shouldProcessEventWithLargeOrderAmount() {
        
        UUID orderId = UUID.randomUUID();
        String customerEmail = "vip@example.com";
        BigDecimal amount = new BigDecimal("10000.50");

        OrderCreatedEvent event = new OrderCreatedEvent(orderId, customerEmail, amount, null);

        
        orderEventsConsumer.handleOrderEvents(event);

        
        verify(emailService).sendOrderConfirmation(
                customerEmail,
                orderId.toString(),
                "10000.50");
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should handle multiple events sequentially")
    void shouldHandleMultipleEventsSequentially() {
        
        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();

        OrderCreatedEvent event1 = new OrderCreatedEvent(orderId1, "customer1@example.com", new BigDecimal("100.00"),
                null);

        OrderCreatedEvent event2 = new OrderCreatedEvent(orderId2, "customer2@example.com", new BigDecimal("200.00"),
                null);

        
        orderEventsConsumer.handleOrderEvents(event1);
        orderEventsConsumer.handleOrderEvents(event2);

        
        verify(emailService, times(2)).sendOrderConfirmation(any(), any(), any());
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should validate notification entity creation with correct fields")
    void shouldValidateNotificationEntityCreationWithCorrectFields() {
        
        UUID orderId = UUID.randomUUID();
        String customerEmail = "test@example.com";
        BigDecimal amount = new BigDecimal("75.25");

        OrderCreatedEvent event = new OrderCreatedEvent(orderId, customerEmail, amount, null);

        
        orderEventsConsumer.handleOrderEvents(event);

        
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification notification = captor.getValue();
        assertThat(notification.getOrderId()).isEqualTo(orderId);
        assertThat(notification.getRecipient()).isEqualTo(customerEmail);
        assertThat(notification.getSubject()).isEqualTo("Order Confirmation");
        assertThat(notification.getType()).isEqualTo("ORDER_CREATED");
        assertThat(notification.getSentAt()).isNotNull();
    }
}
