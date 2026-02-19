package com.oms.orderservice.infrastructure.dlq;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted record of every message that landed in the DLQ.
 * Written by DlqConsumer when a message arrives on order.command.dlq.
 * Admin endpoints expose these for visibility and manual retry.
 */
@Entity
@Table(name = "dlq_records")
@Getter
@NoArgsConstructor
public class DlqRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String sourceService;

    @Column(nullable = false)
    private String originalTopic;

    @Column(nullable = false)
    private String eventType;

    private String aggregateId;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(columnDefinition = "TEXT")
    private String exceptionMessage;

    @Column(nullable = false)
    private Instant failedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DlqStatus status = DlqStatus.PENDING;

    private Instant resolvedAt;

    public static DlqRecord from(String sourceService,
            String originalTopic,
            String eventType,
            String aggregateId,
            String payload,
            String exceptionMessage) {
        DlqRecord r = new DlqRecord();
        r.sourceService = sourceService;
        r.originalTopic = originalTopic;
        r.eventType = eventType;
        r.aggregateId = aggregateId;
        r.payload = payload;
        r.exceptionMessage = exceptionMessage;
        r.failedAt = Instant.now();
        r.status = DlqStatus.PENDING;
        return r;
    }

    public void markRetried() {
        this.status = DlqStatus.RETRIED;
        this.resolvedAt = Instant.now();
    }

    public void markResolved() {
        this.status = DlqStatus.RESOLVED;
        this.resolvedAt = Instant.now();
    }
}
