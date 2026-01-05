package com.oms.inventoryservice.infrastructure.persistence;

import com.oms.inventoryservice.domain.model.Inventory;
import com.oms.inventoryservice.domain.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaInventoryRepository implements InventoryRepository {

    private final SpringDataInventoryRepository springRepo;

    @Override
    public Optional<Inventory> findByProductId(String productId) {
        return springRepo.findByProductId(productId);
    }

    @Override
    public Inventory save(Inventory inventory) {
        return springRepo.save(inventory);
    }
}
