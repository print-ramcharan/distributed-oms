package com.oms.sagaorchestrator.saga;

import com.oms.sagaorchestrator.saga.domain.OrderSaga;
import com.oms.sagaorchestrator.saga.domain.SagaState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for OrderSaga state machine
 * Tests all state transitions and validation logic
 */
class OrderSagaStateTest {

    @Test
    void shouldCreateSagaInStartedState() {
        // Given
        UUID orderId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(1000);

        // When
        OrderSaga saga = new OrderSaga(orderId, amount);

        // Then
        assertThat(saga.getOrderId()).isEqualTo(orderId);
        assertThat(saga.getAmount()).isEqualTo(amount);
        assertThat(saga.getState()).isEqualTo(SagaState.STARTED);
        assertThat(saga.getCreatedAt()).isNotNull();
        assertThat(saga.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldTransitionFromStartedToPaymentRequested() {
        // Given
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));

        // When
        saga.markPaymentRequested();

        // Then
        assertThat(saga.getState()).isEqualTo(SagaState.PAYMENT_REQUESTED);
    }

    @Test
    void shouldTransitionFromPaymentRequestedToPaymentCompleted() {
        // Given
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();

        // When
        saga.markPaymentCompleted();

        // Then
        assertThat(saga.getState()).isEqualTo(SagaState.PAYMENT_COMPLETED);
    }

    @Test
    void shouldTransitionFromPaymentCompletedToInventoryRequested() {
        // Given
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();
        saga.markPaymentCompleted();

        // When
        saga.markInventoryRequested();

        // Then
        assertThat(saga.getState()).isEqualTo(SagaState.INVENTORY_REQUESTED);
    }

    @Test
    void shouldTransitionFromInventoryRequestedToInventoryReserved() {
        // Given
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();
        saga.markPaymentCompleted();
        saga.markInventoryRequested();

        // When
        saga.markInventoryReserved();

        // Then
        assertThat(saga.getState()).isEqualTo(SagaState.INVENTORY_RESERVED);
    }

    @Test
    void shouldTransitionFromInventoryReservedToCompleted() {
        // Given
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();
        saga.markPaymentCompleted();
        saga.markInventoryRequested();
        saga.markInventoryReserved();

        // When
        saga.markCompleted();

        // Then
        assertThat(saga.getState()).isEqualTo(SagaState.COMPLETED);
    }

    @Test
    void shouldHandlePaymentFailureTransition() {
        // Given
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();

        // When
        saga.markPaymentFailed();

        // Then
        assertThat(saga.getState()).isEqualTo(SagaState.PAYMENT_FAILED);
    }

    @Test
    void shouldMarkSagaAsFailedAfterPaymentFailure() {
        // Given
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();
        saga.markPaymentFailed();

        // When
        saga.markFailed();

        // Then
        assertThat(saga.getState()).isEqualTo(SagaState.FAILED);
    }

    @Test
    void shouldHandleInventoryFailureAndCompensation() {
        // Given
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();
        saga.markPaymentCompleted();
        saga.markInventoryRequested();

        // When inventory fails
        saga.markInventoryFailed();

        // Then
        assertThat(saga.getState()).isEqualTo(SagaState.INVENTORY_FAILED);
    }

    @Test
    void shouldTransitionFromInventoryFailedToCompensating() {
        // Given
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();
        saga.markPaymentCompleted();
        saga.markInventoryRequested();
        saga.markInventoryFailed();

        // When
        saga.markCompensating();

        // Then
        assertThat(saga.getState()).isEqualTo(SagaState.COMPENSATING);
    }

    @Test
    void shouldTransitionFromCompensatingToCompensated() {
        // Given
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();
        saga.markPaymentCompleted();
        saga.markInventoryRequested();
        saga.markInventoryFailed();
        saga.markCompensating();

        // When
        saga.markCompensated();

        // Then
        assertThat(saga.getState()).isEqualTo(SagaState.COMPENSATED);
    }

    @Test
    void shouldMarkSagaAsFailedAfterCompensation() {
        // Given
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();
        saga.markPaymentCompleted();
        saga.markInventoryRequested();
        saga.markInventoryFailed();
        saga.markCompensating();
        saga.markCompensated();

        // When
        saga.markFailed();

        // Then
        assertThat(saga.getState()).isEqualTo(SagaState.FAILED);
    }

    @Test
    void shouldRejectInvalidStateTransition_PaymentCompletedWithoutRequest() {
        // Given
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));

        // When/Then
        assertThatThrownBy(saga::markPaymentCompleted)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid saga transition");
    }

    @Test
    void shouldRejectInvalidStateTransition_InventoryRequestedWithoutPaymentCompleted() {
        // Given
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();

        // When/Then
        assertThatThrownBy(saga::markInventoryRequested)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid saga transition");
    }

    @Test
    void shouldRejectInvalidStateTransition_CompletedWithoutInventoryReserved() {
        // Given
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();
        saga.markPaymentCompleted();
        saga.markInventoryRequested();

        // When/Then
        assertThatThrownBy(saga::markCompleted)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid saga transition");
    }

    @Test
    void shouldRejectCompensationFromInvalidState() {
        // Given
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));

        // When/Then
        assertThatThrownBy(saga::markCompensating)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot compensate from state");
    }

    @Test
    void shouldAddItemsToSaga() {
        // Given
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));

        // When
        saga.addItem("product-1", 2, BigDecimal.valueOf(500));

        // Then
        assertThat(saga.getItems()).hasSize(1);
        assertThat(saga.getItems().get(0).getProductId()).isEqualTo("product-1");
        assertThat(saga.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(saga.getItems().get(0).getPrice()).isEqualTo(BigDecimal.valueOf(500));
    }

    @Test
    void shouldMarkItemAsReserved() {
        // Given
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.addItem("product-1", 2, BigDecimal.valueOf(500));

        // When
        saga.markItemReserved("product-1");

        // Then
        assertThat(saga.isItemReserved("product-1")).isTrue();
    }

    @Test
    void shouldReturnTrueWhenAllItemsReserved() {
        // Given
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1500));
        saga.addItem("product-1", 2, BigDecimal.valueOf(500));
        saga.addItem("product-2", 1, BigDecimal.valueOf(500));

        // When
        saga.markItemReserved("product-1");
        saga.markItemReserved("product-2");

        // Then
        assertThat(saga.allItemsReserved()).isTrue();
    }

    @Test
    void shouldReturnFalseWhenNotAllItemsReserved() {
        // Given
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1500));
        saga.addItem("product-1", 2, BigDecimal.valueOf(500));
        saga.addItem("product-2", 1, BigDecimal.valueOf(500));

        // When
        saga.markItemReserved("product-1");
        // product-2 is not reserved

        // Then
        assertThat(saga.allItemsReserved()).isFalse();
    }

    @Test
    void shouldUpdateTimestampOnStateTransition() throws InterruptedException {
        // Given
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        var initialUpdatedAt = saga.getUpdatedAt();

        // Wait a bit to ensure time difference
        Thread.sleep(10);

        // When
        saga.markPaymentRequested();

        // Then
        assertThat(saga.getUpdatedAt()).isAfter(initialUpdatedAt);
    }
}
