package com.oms.fulfillmentservice.api;

import com.oms.fulfillmentservice.domain.FulfillmentTask;
import com.oms.fulfillmentservice.domain.FulfillmentTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/fulfillment")
@RequiredArgsConstructor
public class FulfillmentController {

    private final FulfillmentTaskRepository fulfillmentTaskRepository;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<FulfillmentTask> getFulfillmentByOrder(@PathVariable String orderId) {
        return fulfillmentTaskRepository.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<FulfillmentTask>> listAll() {
        return ResponseEntity.ok(fulfillmentTaskRepository.findAll());
    }
}
