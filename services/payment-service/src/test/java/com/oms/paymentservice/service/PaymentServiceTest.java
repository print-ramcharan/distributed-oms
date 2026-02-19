package com.oms.paymentservice.service;

import com.oms.paymentservice.domain.Payment;
import com.oms.paymentservice.domain.PaymentStatus;
import com.oms.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentService paymentService;

    private UUID orderId;
    private BigDecimal amount;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        amount = BigDecimal.valueOf(100.00);
    }

    @Test
    void shouldCreatePaymentSuccessfully() {
        
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        
        Payment payment = paymentService.createPayment(orderId, amount, "test-idempotency-key");

        
        assertThat(payment).isNotNull();
        assertThat(payment.getOrderId()).isEqualTo(orderId);
        assertThat(payment.getAmount()).isEqualTo(amount);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void shouldReturnExistingPaymentIfAlreadyExists() {
        
        Payment existingPayment = new Payment(orderId, amount);
        when(paymentRepository.findByIdempotencyKey("test-idempotency-key")).thenReturn(Optional.of(existingPayment));

        
        Payment payment = paymentService.createPayment(orderId, amount, "test-idempotency-key");

        
        assertThat(payment).isEqualTo(existingPayment);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void shouldProcessPaymentSuccessfully() {
        
        Payment payment = new Payment(orderId, amount);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        
        Payment processedPayment = paymentService.processPayment(orderId);

        
        assertThat(processedPayment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(paymentRepository).save(payment);
    }

    @Test
    void shouldThrowExceptionWhenPaymentNotFoundForProcessing() {
        
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> paymentService.processPayment(orderId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    void shouldRefundPaymentSuccessfully() {
        
        Payment payment = new Payment(orderId, amount);
        payment.markCompleted();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        
        Payment refundedPayment = paymentService.refundPayment(orderId);

        
        assertThat(refundedPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentRepository).save(payment);
    }

    @Test
    void shouldThrowExceptionWhenPaymentNotFoundForRefund() {
        
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> paymentService.refundPayment(orderId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    void shouldMarkPaymentAsFailed() {
        
        Payment payment = new Payment(orderId, amount);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        
        Payment failedPayment = paymentService.markPaymentFailed(orderId, "Insufficient funds");

        
        assertThat(failedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository).save(payment);
    }
}
