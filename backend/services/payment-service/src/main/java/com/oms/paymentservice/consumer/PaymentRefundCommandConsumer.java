package com.oms.paymentservice.consumer;

import com.oms.eventcontracts.commands.RefundPaymentCommand;
import com.oms.eventcontracts.events.PaymentRefundedEvent;
import com.oms.paymentservice.domain.Payment;
import com.oms.paymentservice.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRefundCommandConsumer {

        private final PaymentRepository paymentRepository;
        private final KafkaTemplate<String, Object> kafkaTemplate;

        @KafkaListener(topics = "payment.refund.command", groupId = "payment-service", containerFactory = "refundKafkaListenerContainerFactory")
        @Transactional
        public void handle(RefundPaymentCommand command) {

                UUID orderId = command.getOrderId();

                log.info(
                                "RefundPaymentCommand received | orderId={} | amount={} | reason={}",
                                orderId, command.getAmount(), command.getReason());

                Payment payment = paymentRepository
                                .findByOrderId(orderId)
                                .orElseThrow(() -> new IllegalStateException("Payment not found for order " + orderId));

                
                if (payment.isRefunded()) {
                        log.info("Refund already completed for order {}", orderId);
                        return;
                }

                
                payment.refund(command.getAmount());
                paymentRepository.save(payment);

                
                kafkaTemplate.send(
                                "payment.refunded",
                                orderId.toString(),
                                new PaymentRefundedEvent(
                                                orderId,
                                                UUID.fromString(String.valueOf(payment.getId())), 
                                                command.getAmount(),
                                                command.getReason(),
                                                Instant.now()));

                log.info(
                                "Payment refunded | orderId={} | paymentId={} | amount={}",
                                orderId, payment.getId(), command.getAmount());
        }
}
