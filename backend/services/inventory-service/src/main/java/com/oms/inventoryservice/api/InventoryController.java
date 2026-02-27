package com.oms.inventoryservice.api;

import com.oms.inventoryservice.domain.model.Inventory;
import com.oms.inventoryservice.domain.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryRepository inventoryRepository;
    private final com.oms.inventoryservice.service.InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<Inventory> createItem(@RequestBody CreateInventoryRequest request) {
        Inventory inventory = new Inventory(request.getProductId(), request.getInitialStock());
        return ResponseEntity.ok(inventoryRepository.save(inventory));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Inventory> getItem(@PathVariable String id) {
        return inventoryRepository.findByProductId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<java.util.List<Inventory>> listAll() {
        return ResponseEntity.ok(inventoryService.findAll());
    }

    @PostMapping("/{id}/add")
    public ResponseEntity<Void> addStock(@PathVariable String id, @RequestParam int quantity) {
        inventoryService.addStock(id, quantity);
        return ResponseEntity.ok().build();
    }
}
