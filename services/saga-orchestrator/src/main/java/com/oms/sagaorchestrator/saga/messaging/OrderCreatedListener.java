package com.oms.sagaorchestrator.saga.messaging;

import com.oms.eventcontracts.commands.AdvanceOrderProgressCommand;
import com.oms.eventcontracts.commands.InitiatePaymentCommand;
import com.oms.eventcontracts.enums.OrderProgress;
import com.oms.eventcontracts.events.OrderCreatedEvent;
import com.oms.eventcontracts.events.OrderItemDTO;
import com.oms.sagaorchestrator.saga.domain.OrderSaga;
import com.oms.sagaorchestrator.saga.domain.SagaState;
import com.oms.sagaorchestrator.saga.repository.OrderSagaRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Listens to order.event.created and starts the saga by dispatching
 * an InitiatePaymentCommand to the payment service via Kafka.
 *
 * Circuit Breaker protects against Kafka broker-level failures:
 * if kafkaTemplate.send() starts throwing (broker unavailable, topic errors),
 * the breaker opens and the saga fails fast instead of blocking Kafka consumer
 * threads or retrying indefinitely.
 *
 * For payment-service APPLICATION failures (process crash, OOM),
 * Kafka's durability means messages wait in the topic and are processed
 * when payment-service recovers — that's intentional temporal decoupling.
 * The @Retryable in PaymentService handles transient failures within
 * payment-service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedListener {

        private final OrderSagaRepository sagaRepository;
        private final KafkaTemplate<String, Object> kafkaTemplate;
        private final CircuitBreakerRegistry circuitBreakerRegistry;

        private CircuitBreaker paymentCircuitBreaker;

        @PostConstruct
        public void init() {
                paymentCircuitBreaker = circuitBreakerRegistry.circuitBreaker("payment-service");
                // Log state transitions for observability
                paymentCircuitBreaker.getEventPublisher()
                                .onStateTransition(event -> log.warn(
                                                "🔌 Circuit Breaker [payment-service] state changed: {} → {}",
                                                event.getStateTransition().getFromState(),
                                                event.getStateTransition().getToState()));
        }

        @KafkaListener(topics = "order.event.created", groupId = "saga-orchestrator")
        @Transactional
        public void handle(OrderCreatedEvent event) {

                OrderSaga saga = sagaRepository.findById(event.getOrderId())
                                .orElseGet(() -> {
                                        OrderSaga s = new OrderSaga(event.getOrderId(), event.getAmount());
                                        for (OrderItemDTO item : event.getItems()) {
                                                s.addItem(item.getProductId(), item.getQuantity(), item.getPrice());
                                        }
                                        return sagaRepository.save(s);
                                });

                // Idempotency guard
                if (saga.getState() != SagaState.STARTED) {
                        log.info("⏭️ Saga already progressed for orderId={}, skipping", event.getOrderId());
                        return;
                }

                try {
                        // Wrap Kafka publishes in a circuit breaker.
                        // Trips if the Kafka broker itself becomes unreachable or topic errors
                        // accumulate — prevents blocking consumer threads indefinitely.
                        CircuitBreaker.decorateRunnable(paymentCircuitBreaker, () -> {
                                kafkaTemplate.send(
                                                "order.command.advance-progress",
                                                String.valueOf(event.getOrderId()),
                                                new AdvanceOrderProgressCommand(event.getOrderId(),
                                                                OrderProgress.AWAITING_PAYMENT));

                                kafkaTemplate.send(
                                                "payment.initiate",
                                                event.getOrderId().toString(),
                                                new InitiatePaymentCommand(event.getOrderId(), event.getAmount()));
                        }).run();

                        saga.markPaymentRequested();
                        sagaRepository.save(saga);
                        log.info("✅ Payment command dispatched for orderId={}", event.getOrderId());

                } catch (CallNotPermittedException e) {
                        // Circuit is OPEN — fail fast, trigger compensation
                        log.error("🔴 Circuit OPEN for payment-service — failing saga fast for orderId={}",
                                        event.getOrderId());
                        handlePaymentDispatchFailure(saga);

                } catch (Exception e) {
                        log.error("❌ Failed to dispatch payment command for orderId={}: {}",
                                        event.getOrderId(), e.getMessage(), e);
                        handlePaymentDispatchFailure(saga);
                }
        }

        /**
         * When payment dispatch fails (circuit open or exception),
         * mark the saga as payment-failed so the PaymentFailedListener
         * can trigger the compensation flow (cancel order, notify customer).
         */
        private void handlePaymentDispatchFailure(OrderSaga saga) {
                try {
                        saga.markPaymentFailed();
                        sagaRepository.save(saga);
                        log.warn("⚠️ Saga marked PAYMENT_FAILED due to dispatch failure for orderId={}",
                                        saga.getOrderId());
                } catch (Exception ex) {
                        log.error("❌ Could not update saga state after dispatch failure", ex);
                }
        }
}
