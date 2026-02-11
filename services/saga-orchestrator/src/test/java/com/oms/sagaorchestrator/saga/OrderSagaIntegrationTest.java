package com.oms.sagaorchestrator.saga;

import com.oms.sagaorchestrator.saga.domain.OrderSaga;
import com.oms.sagaorchestrator.saga.domain.SagaState;
import com.oms.sagaorchestrator.saga.repository.OrderSagaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for OrderSaga persistence
 * Tests database operations, transactions, and state persistence
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderSagaIntegrationTest {

    @Autowired
    private OrderSagaRepository sagaRepository;

    @Test
    void shouldPersistNewSagaToDatabase() {
        // Given
        UUID orderId = UUID.randomUUID();
        OrderSaga saga = new OrderSaga(orderId, BigDecimal.valueOf(1000));
        saga.addItem("product-1", 2, BigDecimal.valueOf(500));

        // When
        OrderSaga saved = sagaRepository.save(saga);

        // Then
        assertThat(saved.getOrderId()).isEqualTo(orderId);
        assertThat(saved.getState()).isEqualTo(SagaState.STARTED);
        assertThat(saved.getItems()).hasSize(1);
    }

    @Test
    void shouldUpdateSagaState() {
        // Given
        UUID orderId = UUID.randomUUID();
        OrderSaga saga = new OrderSaga(orderId, BigDecimal.valueOf(1000));
        sagaRepository.save(saga);

        // When
        saga.markPaymentRequested();
        sagaRepository.save(saga);

        // Then
        OrderSaga retrieved = sagaRepository.findById(orderId).orElseThrow();
        assertThat(retrieved.getState()).isEqualTo(SagaState.PAYMENT_REQUESTED);
    }

    @Test
    void shouldMaintainSagaItemsAcrossTransitions() {
        // Given
        UUID orderId = UUID.randomUUID();
        OrderSaga saga = new OrderSaga(orderId, BigDecimal.valueOf(1500));
        saga.addItem("product-1", 2, BigDecimal.valueOf(500));
        saga.addItem("product-2", 1, BigDecimal.valueOf(500));
        sagaRepository.save(saga);

        // When - transition through multiple states
        saga.markPaymentRequested();
        saga.markPaymentCompleted();
        saga.markInventoryRequested();
        sagaRepository.save(saga);

        // Then
        OrderSaga retrieved = sagaRepository.findById(orderId).orElseThrow();
        assertThat(retrieved.getItems()).hasSize(2);
        assertThat(retrieved.getState()).isEqualTo(SagaState.INVENTORY_REQUESTED);
    }

    @Test
    void shouldHandleCompensationFlowPersistence() {
        // Given
        UUID orderId = UUID.randomUUID();
        OrderSaga saga = new OrderSaga(orderId, BigDecimal.valueOf(1000));
        saga.markPaymentRequested();
        saga.markPaymentCompleted();
        saga.markInventoryRequested();
        sagaRepository.save(saga);

        // When - compensation flow
        saga.markInventoryFailed();
        saga.markCompensating();
        saga.markCompensated();
        saga.markFailed();
        sagaRepository.save(saga);

        // Then
        OrderSaga retrieved = sagaRepository.findById(orderId).orElseThrow();
        assertThat(retrieved.getState()).isEqualTo(SagaState.FAILED);
    }

    @Test
    void shouldPreserveTimestamps() {
        // Given
        UUID orderId = UUID.randomUUID();
        OrderSaga saga = new OrderSaga(orderId, BigDecimal.valueOf(1000));
        OrderSaga saved = sagaRepository.save(saga);

        // Then
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    void shouldUpdateTimestampOnStateChange() throws InterruptedException {
        // Given
        UUID orderId = UUID.randomUUID();
        OrderSaga saga = new OrderSaga(orderId, BigDecimal.valueOf(1000));
        OrderSaga saved = sagaRepository.save(saga);
        var initialUpdatedAt = saved.getUpdatedAt();

        Thread.sleep(10);

        // When
        saga.markPaymentRequested();
        OrderSaga updated = sagaRepository.save(saga);

        // Then
        assertThat(updated.getUpdatedAt()).isAfter(initialUpdatedAt);
        assertThat(updated.getCreatedAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    void shouldFindSagaByOrderId() {
        // Given
        UUID orderId = UUID.randomUUID();
        OrderSaga saga = new OrderSaga(orderId, BigDecimal.valueOf(1000));
        sagaRepository.save(saga);

        // When
        var found = sagaRepository.findById(orderId);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo(orderId);
    }

    @Test
    void shouldReturnEmptyWhenSagaNotFound() {
        // Given
        UUID nonExistentOrderId = UUID.randomUUID();

        // When
        var found = sagaRepository.findById(nonExistentOrderId);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldSuccessfullyCompleteHappyPathSaga() {
        // Given
        UUID orderId = UUID.randomUUID();
        OrderSaga saga = new OrderSaga(orderId, BigDecimal.valueOf(1000));
        saga.addItem("product-1", 1, BigDecimal.valueOf(1000));

        // When - Happy path flow
        sagaRepository.save(saga);
        saga.markPaymentRequested();
        saga.markPaymentCompleted();
        saga.markInventoryRequested();
        saga.markItemReserved("product-1");
        saga.markInventoryReserved();
        saga.markCompleted();
        OrderSaga completed = sagaRepository.save(saga);

        // Then
        assertThat(completed.getState()).isEqualTo(SagaState.COMPLETED);
        assertThat(completed.allItemsReserved()).isTrue();
    }
}
