package com.oms.sagaorchestrator.api;

import com.oms.sagaorchestrator.saga.repository.OrderSagaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/sagas")
@RequiredArgsConstructor
public class SagaController {

    private final OrderSagaRepository orderSagaRepository;

    @GetMapping("/{orderId}")
    public ResponseEntity<com.oms.sagaorchestrator.saga.domain.OrderSaga> getSagaByOrder(@PathVariable UUID orderId) {
        return orderSagaRepository.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
