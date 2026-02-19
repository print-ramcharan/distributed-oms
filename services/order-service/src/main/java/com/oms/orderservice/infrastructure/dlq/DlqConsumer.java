package com.oms.orderservice.infrastructure.dlq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes messages from order.command.dlq — the topic that
 * Spring Kafka's DeadLetterPublishingRecoverer routes to after
 * all retry attempts are exhausted.
 *
 * Each message is persisted as a DlqRecord so that:
 * - Ops can view failures via GET /admin/dlq
 * - Failed messages can be manually retried via POST /admin/dlq/{id}/retry
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DlqConsumer {

    private final DlqRecordRepository dlqRecordRepository;

    @KafkaListener(topics = "order.command.dlq", groupId = "order-service-dlq", containerFactory = "dlqKafkaListenerContainerFactory")
    @Transactional
    public void handle(ConsumerRecord<String, String> record) {
        log.warn("📨 DLQ message received | topic={} partition={} offset={} key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        // Extract Spring Kafka DLQ exception headers
        String exceptionMessage = extractHeader(record, "kafka_dlt-exception-message");
        String originalTopic = extractHeader(record, "kafka_dlt-original-topic");
        String originalOffset = extractHeader(record, "kafka_dlt-original-offset");

        String payload = record.value() != null ? record.value() : "<null>";

        DlqRecord dlqRecord = DlqRecord.from(
                "order-service",
                originalTopic != null ? originalTopic : "order.command.advance-progress",
                "AdvanceOrderProgressCommand",
                record.key(),
                payload,
                exceptionMessage);

        dlqRecordRepository.save(dlqRecord);

        log.warn("⚠️  DLQ record persisted | id={} originalTopic={} originalOffset={}",
                dlqRecord.getId(), originalTopic, originalOffset);
    }

    private String extractHeader(ConsumerRecord<?, ?> record, String headerName) {
        if (record.headers() == null)
            return null;
        var header = record.headers().lastHeader(headerName);
        return header != null ? new String(header.value()) : null;
    }
}
