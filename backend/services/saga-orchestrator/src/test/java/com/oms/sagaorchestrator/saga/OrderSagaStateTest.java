package com.oms.sagaorchestrator.saga;

import com.oms.sagaorchestrator.saga.domain.OrderSaga;
import com.oms.sagaorchestrator.saga.domain.SagaState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;


class OrderSagaStateTest {

    @Test
    void shouldCreateSagaInStartedState() {
        
        UUID orderId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(1000);

        
        OrderSaga saga = new OrderSaga(orderId, amount);

        
        assertThat(saga.getOrderId()).isEqualTo(orderId);
        assertThat(saga.getAmount()).isEqualTo(amount);
        assertThat(saga.getState()).isEqualTo(SagaState.STARTED);
        assertThat(saga.getCreatedAt()).isNotNull();
        assertThat(saga.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldTransitionFromStartedToPaymentRequested() {
        
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));

        
        saga.markPaymentRequested();

        
        assertThat(saga.getState()).isEqualTo(SagaState.PAYMENT_REQUESTED);
    }

    @Test
    void shouldTransitionFromPaymentRequestedToPaymentCompleted() {
        
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();

        
        saga.markPaymentCompleted();

        
        assertThat(saga.getState()).isEqualTo(SagaState.PAYMENT_COMPLETED);
    }

    @Test
    void shouldTransitionFromPaymentCompletedToInventoryRequested() {
        
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();
        saga.markPaymentCompleted();

        
        saga.markInventoryRequested();

        
        assertThat(saga.getState()).isEqualTo(SagaState.INVENTORY_REQUESTED);
    }

    @Test
    void shouldTransitionFromInventoryRequestedToInventoryReserved() {
        
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();
        saga.markPaymentCompleted();
        saga.markInventoryRequested();

        
        saga.markInventoryReserved();

        
        assertThat(saga.getState()).isEqualTo(SagaState.INVENTORY_RESERVED);
    }

    @Test
    void shouldTransitionFromInventoryReservedToCompleted() {
        
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();
        saga.markPaymentCompleted();
        saga.markInventoryRequested();
        saga.markInventoryReserved();

        
        saga.markCompleted();

        
        assertThat(saga.getState()).isEqualTo(SagaState.COMPLETED);
    }

    @Test
    void shouldHandlePaymentFailureTransition() {
        
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();

        
        saga.markPaymentFailed();

        
        assertThat(saga.getState()).isEqualTo(SagaState.PAYMENT_FAILED);
    }

    @Test
    void shouldMarkSagaAsFailedAfterPaymentFailure() {
        
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();
        saga.markPaymentFailed();

        
        saga.markFailed();

        
        assertThat(saga.getState()).isEqualTo(SagaState.FAILED);
    }

    @Test
    void shouldHandleInventoryFailureAndCompensation() {
        
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();
        saga.markPaymentCompleted();
        saga.markInventoryRequested();

        
        saga.markInventoryFailed();

        
        assertThat(saga.getState()).isEqualTo(SagaState.INVENTORY_FAILED);
    }

    @Test
    void shouldTransitionFromInventoryFailedToCompensating() {
        
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();
        saga.markPaymentCompleted();
        saga.markInventoryRequested();
        saga.markInventoryFailed();

        
        saga.markCompensating();

        
        assertThat(saga.getState()).isEqualTo(SagaState.COMPENSATING);
    }

    @Test
    void shouldTransitionFromCompensatingToCompensated() {
        
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();
        saga.markPaymentCompleted();
        saga.markInventoryRequested();
        saga.markInventoryFailed();
        saga.markCompensating();

        
        saga.markCompensated();

        
        assertThat(saga.getState()).isEqualTo(SagaState.COMPENSATED);
    }

    @Test
    void shouldMarkSagaAsFailedAfterCompensation() {
        
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();
        saga.markPaymentCompleted();
        saga.markInventoryRequested();
        saga.markInventoryFailed();
        saga.markCompensating();
        saga.markCompensated();

        
        saga.markFailed();

        
        assertThat(saga.getState()).isEqualTo(SagaState.FAILED);
    }

    @Test
    void shouldRejectInvalidStateTransition_PaymentCompletedWithoutRequest() {
        
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));

        
        assertThatThrownBy(saga::markPaymentCompleted)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid saga transition");
    }

    @Test
    void shouldRejectInvalidStateTransition_InventoryRequestedWithoutPaymentCompleted() {
        
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();

        
        assertThatThrownBy(saga::markInventoryRequested)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid saga transition");
    }

    @Test
    void shouldRejectInvalidStateTransition_CompletedWithoutInventoryReserved() {
        
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.markPaymentRequested();
        saga.markPaymentCompleted();
        saga.markInventoryRequested();

        
        assertThatThrownBy(saga::markCompleted)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid saga transition");
    }

    @Test
    void shouldRejectCompensationFromInvalidState() {
        
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));

        
        assertThatThrownBy(saga::markCompensating)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot compensate from state");
    }

    @Test
    void shouldAddItemsToSaga() {
        
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));

        
        saga.addItem("product-1", 2, BigDecimal.valueOf(500));

        
        assertThat(saga.getItems()).hasSize(1);
        assertThat(saga.getItems().get(0).getProductId()).isEqualTo("product-1");
        assertThat(saga.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(saga.getItems().get(0).getPrice()).isEqualTo(BigDecimal.valueOf(500));
    }

    @Test
    void shouldMarkItemAsReserved() {
        
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        saga.addItem("product-1", 2, BigDecimal.valueOf(500));

        
        saga.markItemReserved("product-1");

        
        assertThat(saga.isItemReserved("product-1")).isTrue();
    }

    @Test
    void shouldReturnTrueWhenAllItemsReserved() {
        
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1500));
        saga.addItem("product-1", 2, BigDecimal.valueOf(500));
        saga.addItem("product-2", 1, BigDecimal.valueOf(500));

        
        saga.markItemReserved("product-1");
        saga.markItemReserved("product-2");

        
        assertThat(saga.allItemsReserved()).isTrue();
    }

    @Test
    void shouldReturnFalseWhenNotAllItemsReserved() {
        
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1500));
        saga.addItem("product-1", 2, BigDecimal.valueOf(500));
        saga.addItem("product-2", 1, BigDecimal.valueOf(500));

        
        saga.markItemReserved("product-1");
        

        
        assertThat(saga.allItemsReserved()).isFalse();
    }

    @Test
    void shouldUpdateTimestampOnStateTransition() throws InterruptedException {
        
        OrderSaga saga = new OrderSaga(UUID.randomUUID(), BigDecimal.valueOf(1000));
        var initialUpdatedAt = saga.getUpdatedAt();

        
        Thread.sleep(10);

        
        saga.markPaymentRequested();

        
        assertThat(saga.getUpdatedAt()).isAfter(initialUpdatedAt);
    }
}
