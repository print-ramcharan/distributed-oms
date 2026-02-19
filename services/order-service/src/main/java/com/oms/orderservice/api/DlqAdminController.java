package com.oms.orderservice.api;

import com.oms.orderservice.infrastructure.dlq.DlqRecord;
import com.oms.orderservice.infrastructure.dlq.DlqRecordRepository;
import com.oms.orderservice.infrastructure.dlq.DlqStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin REST API for inspecting and retrying DLQ messages.
 *
 * GET /admin/dlq — list all DLQ records (newest first)
 * GET /admin/dlq?status=PENDING — filter by status
 * POST /admin/dlq/{id}/retry — re-publish payload back to original topic
 * POST /admin/dlq/{id}/resolve — mark as resolved without retrying
 */
@RestController
@RequestMapping("/admin/dlq")
@RequiredArgsConstructor
@Slf4j
public class DlqAdminController {

    private final DlqRecordRepository dlqRecordRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @GetMapping
    public List<DlqRecord> list(
            @RequestParam(required = false) DlqStatus status) {
        if (status != null) {
            return dlqRecordRepository.findByStatus(status);
        }
        return dlqRecordRepository.findAllByOrderByFailedAtDesc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DlqRecord> get(@PathVariable UUID id) {
        return dlqRecordRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Re-publishes the raw payload back to the original topic so the
     * consumer can try processing it again.
     */
    @PostMapping("/{id}/retry")
    @Transactional
    public ResponseEntity<Map<String, String>> retry(@PathVariable UUID id) {
        return dlqRecordRepository.findById(id)
                .map(record -> {
                    kafkaTemplate.send(
                            record.getOriginalTopic(),
                            record.getAggregateId(),
                            record.getPayload());
                    record.markRetried();
                    dlqRecordRepository.save(record);

                    log.info("♻️  DLQ retry triggered | id={} topic={}", id, record.getOriginalTopic());
                    return ResponseEntity.ok(Map.of(
                            "status", "retried",
                            "topic", record.getOriginalTopic(),
                            "recordId", id.toString()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Marks the DLQ record as resolved without retrying
     * (e.g. it was an expected failure or manually handled).
     */
    @PostMapping("/{id}/resolve")
    @Transactional
    public ResponseEntity<Map<String, String>> resolve(@PathVariable UUID id) {
        return dlqRecordRepository.findById(id)
                .map(record -> {
                    record.markResolved();
                    dlqRecordRepository.save(record);

                    log.info("✅ DLQ record resolved | id={}", id);
                    return ResponseEntity.ok(Map.of(
                            "status", "resolved",
                            "recordId", id.toString()));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
