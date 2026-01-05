package com.oms.inventoryservice.infrastructure.persistence;

import com.oms.inventoryservice.domain.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataInventoryRepository
        extends JpaRepository<Inventory, String> {

    Optional<Inventory> findByProductId(String productId);
}
