package com.oms.sagaorchestrator.saga.messaging;

import com.oms.eventcontracts.events.PaymentRefundedEvent;
import com.oms.sagaorchestrator.saga.domain.OrderSaga;
import com.oms.sagaorchestrator.saga.domain.SagaState;
import com.oms.sagaorchestrator.saga.repository.OrderSagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRefundedListener {

    private final OrderSagaRepository sagaRepository;

    @KafkaListener(topics = "payment.refunded", groupId = "saga-orchestrator", containerFactory = "paymentRefundedKafkaListenerContainerFactory")
    @Transactional
    public void handle(PaymentRefundedEvent event) {
        UUID orderId = event.getOrderId();
        log.info("💰 PaymentRefundedEvent received | orderId={} | reason={}", orderId, event.getReason());

        OrderSaga saga = sagaRepository.findById(orderId).orElse(null);
        if (saga == null) {
            log.warn("⚠️ Saga not found for order {}", orderId);
            return;
        }

        if (saga.getState() != SagaState.COMPENSATING) {
            log.warn("⚠️ Ignoring refund - saga not in COMPENSATING state | currentState={}", saga.getState());
            return;
        }

        log.info("→ Transitioning saga to COMPENSATED (Rollback Complete)");
        saga.markCompensated();
        saga.markFailed(); // Transition to final FAILED state
        sagaRepository.saveAndFlush(saga);
        log.info("✅ Saga fully resolved as FAILED | orderId={}", orderId);
    }
}
