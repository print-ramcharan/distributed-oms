package com.oms.inventoryservice.domain.repository;

import com.oms.inventoryservice.domain.model.Inventory;

import java.util.Optional;

public interface InventoryRepository {

    Optional<Inventory> findByProductId(String productId);

    Inventory save(Inventory inventory);
}
