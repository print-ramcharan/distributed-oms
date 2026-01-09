package com.oms.inventoryservice.infrastructure.persistence;

import com.oms.inventoryservice.domain.model.Inventory;
import com.oms.inventoryservice.domain.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
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

    @Override
    public List<Inventory> findAllByProductIdIn(List<String> productIds) {
        // Delegates to the magic JPA method we defined in step 1
        return springRepo.findByProductIdIn(productIds);
    }

    @Override
    public void saveAll(Collection<Inventory> values) {
        // JpaRepository.saveAll() accepts Iterables, so Collection works fine
        springRepo.saveAll(values);
    }
}