package com.oms.inventoryservice.service;

import com.oms.inventoryservice.domain.Inventory;
import com.oms.inventoryservice.domain.InventoryReservation;
import com.oms.inventoryservice.repository.InventoryRepository;
import com.oms.inventoryservice.repository.InventoryReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InventoryService
 * Tests inventory reservation, release, and concurrency handling
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryReservationRepository reservationRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private String productId;
    private UUID orderId;
    private int quantity;

    @BeforeEach
    void setUp() {
        productId = "product-123";
        orderId = UUID.randomUUID();
        quantity = 5;
    }

    @Test
    void shouldReserveInventorySuccessfullyWhenStockAvailable() {
        // Given
        Inventory inventory = new Inventory(productId, 100, 0);
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.save(any(InventoryReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        InventoryReservation reservation = inventoryService.reserveInventory(orderId, productId, quantity);

        // Then
        assertThat(reservation).isNotNull();
        assertThat(reservation.getOrderId()).isEqualTo(orderId);
        assertThat(reservation.getProductId()).isEqualTo(productId);
        assertThat(reservation.getQuantity()).isEqualTo(quantity);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(95);
        assertThat(inventory.getReservedQuantity()).isEqualTo(5);
        verify(inventoryRepository).save(inventory);
        verify(reservationRepository).save(any(InventoryReservation.class));
    }

    @Test
    void shouldThrowExceptionWhenInsufficientStock() {
        // Given
        Inventory inventory = new Inventory(productId, 3, 0);
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));

        // When/Then
        assertThatThrownBy(() -> inventoryService.reserveInventory(orderId, productId, quantity))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void shouldThrowExceptionWhenProductNotFound() {
        // Given
        when(inventoryRepository.findById(productId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> inventoryService.reserveInventory(orderId, productId, quantity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void shouldReleaseReservationSuccessfully() {
        // Given
        Inventory inventory = new Inventory(productId, 95, 5);
        InventoryReservation reservation = new InventoryReservation(orderId, productId, quantity);
        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        inventoryService.releaseReservation(orderId);

        // Then
        assertThat(inventory.getAvailableQuantity()).isEqualTo(100);
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        verify(inventoryRepository).save(inventory);
        verify(reservationRepository).delete(reservation);
    }

    @Test
    void shouldThrowExceptionWhenReservationNotFoundForRelease() {
        // Given
        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> inventoryService.releaseReservation(orderId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reservation not found");
    }

    @Test
    void shouldConfirmReservationSuccessfully() {
        // Given
        InventoryReservation reservation = new InventoryReservation(orderId, productId, quantity);
        Inventory inventory = new Inventory(productId, 95, 5);
        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        inventoryService.confirmReservation(orderId);

        // Then
        assertThat(reservation.isConfirmed()).isTrue();
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        verify(reservationRepository).save(reservation);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void shouldReturnExistingReservationIfAlreadyExists() {
        // Given
        InventoryReservation existingReservation = new InventoryReservation(orderId, productId, quantity);
        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingReservation));

        // When
        InventoryReservation reservation = inventoryService.reserveInventory(orderId, productId, quantity);

        // Then
        assertThat(reservation).isEqualTo(existingReservation);
        verify(inventoryRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldCheckInventoryAvailability() {
        // Given
        Inventory inventory = new Inventory(productId, 100, 0);
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));

        // When
        boolean available = inventoryService.isAvailable(productId, quantity);

        // Then
        assertThat(available).isTrue();
    }

    @Test
    void shouldReturnFalseWhenInventoryNotAvailable() {
        // Given
        Inventory inventory = new Inventory(productId, 3, 0);
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));

        // When
        boolean available = inventoryService.isAvailable(productId, quantity);

        // Then
        assertThat(available).isFalse();
    }
}
