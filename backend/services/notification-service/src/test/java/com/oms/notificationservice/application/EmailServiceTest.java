package com.oms.notificationservice.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Unit Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@distributed-oms.com");
    }

    @Test
    @DisplayName("Should send order confirmation email successfully")
    void shouldSendOrderConfirmationEmail() {
        
        String to = "customer@example.com";
        String orderId = "order-123";
        String amount = "100.00";

        
        emailService.sendOrderConfirmation(to, orderId, amount);

        
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getTo()).containsExactly(to);
        assertThat(sentMessage.getFrom()).isEqualTo("noreply@distributed-oms.com");
        assertThat(sentMessage.getSubject()).isEqualTo("Order Confirmation: order-123");
        assertThat(sentMessage.getText()).contains("Order ID: order-123");
        assertThat(sentMessage.getText()).contains("Total Amount: 100.00");
    }

    @Test
    @DisplayName("Should handle email sending failure gracefully")
    void shouldHandleEmailSendingFailureGracefully() {
        
        String to = "customer@example.com";
        String orderId = "order-123";
        String amount = "100.00";

        doThrow(new RuntimeException("SMTP server unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        
        emailService.sendOrderConfirmation(to, orderId, amount);

        
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should format email with correct content structure")
    void shouldFormatEmailWithCorrectContentStructure() {
        
        String to = "test@example.com";
        String orderId = "ORD-456";
        String amount = "250.75";

        
        emailService.sendOrderConfirmation(to, orderId, amount);

        
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        String text = message.getText();

        assertThat(text).contains("Thank you for your order!");
        assertThat(text).contains("Order ID: ORD-456");
        assertThat(text).contains("Total Amount: 250.75");
        assertThat(text).contains("We will notify you when it ships.");
    }

    @Test
    @DisplayName("Should send email with multiple recipients")
    void shouldSendEmailWithCustomerEmail() {
        
        String to = "john.doe@example.com";
        String orderId = "ORD-789";
        String amount = "500.00";

        
        emailService.sendOrderConfirmation(to, orderId, amount);

        
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getTo()).containsExactly("john.doe@example.com");
    }

    @Test
    @DisplayName("Should use correct from email address")
    void shouldUseCorrectFromEmailAddress() {
        
        String to = "customer@example.com";
        String orderId = "order-123";
        String amount = "100.00";

        
        emailService.sendOrderConfirmation(to, orderId, amount);

        
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getFrom()).isEqualTo("noreply@distributed-oms.com");
    }

    @Test
    @DisplayName("Should include subject with order ID")
    void shouldIncludeSubjectWithOrderId() {
        
        String to = "customer@example.com";
        String orderId = "ORD-XYZ-999";
        String amount = "75.50";

        
        emailService.sendOrderConfirmation(to, orderId, amount);

        
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getSubject()).isEqualTo("Order Confirmation: ORD-XYZ-999");
    }

    @Test
    @DisplayName("Should not throw exception when mail sender fails")
    void shouldNotThrowExceptionWhenMailSenderFails() {
        
        doThrow(new RuntimeException("Network error"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        
        emailService.sendOrderConfirmation("test@example.com", "order-123", "100");

        
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
