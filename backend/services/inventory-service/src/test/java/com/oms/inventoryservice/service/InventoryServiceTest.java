package com.oms.inventoryservice.service;

import com.oms.inventoryservice.domain.model.Inventory;
import com.oms.inventoryservice.domain.model.InventoryReservation;
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
        
        Inventory inventory = new Inventory(productId, 100);
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.save(any(InventoryReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        
        InventoryReservation reservation = inventoryService.reserveInventory(orderId, productId, quantity);

        
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
        
        Inventory inventory = new Inventory(productId, 3);
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));

        
        assertThatThrownBy(() -> inventoryService.reserveInventory(orderId, productId, quantity))
                .isInstanceOf(Inventory.InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void shouldThrowExceptionWhenProductNotFound() {
        
        when(inventoryRepository.findById(productId)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> inventoryService.reserveInventory(orderId, productId, quantity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void shouldReleaseReservationSuccessfully() {
        
        Inventory inventory = new Inventory(productId, 100);
        
        inventory.confirmReservation(0); 
        inventory.setAvailableQuantity(95);
        inventory.setReservedQuantity(5);

        InventoryReservation reservation = new InventoryReservation(orderId, productId, quantity);
        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        
        inventoryService.releaseReservation(orderId);

        
        assertThat(inventory.getAvailableQuantity()).isEqualTo(100);
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        verify(inventoryRepository).save(inventory);

        
        assertThat(reservation.getStatus()).isEqualTo(InventoryReservation.ReservationStatus.RELEASED);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void shouldThrowExceptionWhenReservationNotFoundForRelease() {
        
        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> inventoryService.releaseReservation(orderId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reservation not found");
    }

    @Test
    void shouldConfirmReservationSuccessfully() {
        
        InventoryReservation reservation = new InventoryReservation(orderId, productId, quantity);
        Inventory inventory = new Inventory(productId, 100);
        inventory.setAvailableQuantity(95);
        inventory.setReservedQuantity(5);

        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        
        inventoryService.confirmReservation(orderId);

        
        assertThat(reservation.getStatus()).isEqualTo(InventoryReservation.ReservationStatus.CONFIRMED);
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        assertThat(inventory.getTotalQuantity()).isEqualTo(95); 
        verify(reservationRepository).save(reservation);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void shouldReturnExistingReservationIfAlreadyExists() {
        
        InventoryReservation existingReservation = new InventoryReservation(orderId, productId, quantity);
        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingReservation));

        
        InventoryReservation reservation = inventoryService.reserveInventory(orderId, productId, quantity);

        
        assertThat(reservation).isEqualTo(existingReservation);
        verify(inventoryRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldCheckInventoryAvailability() {
        
        Inventory inventory = new Inventory(productId, 100);
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));

        
        boolean available = inventoryService.isAvailable(productId, quantity);

        
        assertThat(available).isTrue();
    }

    @Test
    void shouldReturnFalseWhenInventoryNotAvailable() {
        
        Inventory inventory = new Inventory(productId, 3);
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));

        
        boolean available = inventoryService.isAvailable(productId, quantity);

        
        assertThat(available).isFalse();
    }

    @Test
    void shouldReleaseExpiredReservations() {
        
        InventoryReservation expiredReservation = new InventoryReservation(orderId, productId, quantity);
        
        

        Inventory inventory = new Inventory(productId, 100);
        inventory.confirmReservation(0);
        inventory.setAvailableQuantity(95);
        inventory.setReservedQuantity(5);

        when(reservationRepository.findByStatusAndExpiresAtBefore(
                eq(InventoryReservation.ReservationStatus.RESERVED), any()))
                .thenReturn(java.util.List.of(expiredReservation));

        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.of(expiredReservation));
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        
        inventoryService.releaseExpiredReservations();

        
        InventoryReservation released = expiredReservation; 
        assertThat(released.getStatus()).isEqualTo(InventoryReservation.ReservationStatus.RELEASED);
        verify(reservationRepository)
                .findByStatusAndExpiresAtBefore(eq(InventoryReservation.ReservationStatus.RESERVED), any());
        verify(inventoryRepository).save(inventory);
        verify(reservationRepository, atLeastOnce()).save(any(InventoryReservation.class));
    }
}
