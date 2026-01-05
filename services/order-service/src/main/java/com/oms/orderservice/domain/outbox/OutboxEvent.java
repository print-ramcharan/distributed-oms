package com.oms.orderservice.domain.outbox;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode; // Import added
import org.hibernate.type.SqlTypes;           // Import added

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    // --- FIX APPLIED HERE ---
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    protected OutboxEvent() {}

    private OutboxEvent(
            UUID aggregateId,
            String aggregateType,
            String eventType,
            String payload
    ) {
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.NEW;
        this.createdAt = Instant.now();
        this.retryCount = 0;
        this.nextRetryAt = Instant.now();

    }

    public static OutboxEvent create(
            UUID aggregateId,
            String aggregateType,
            String eventType,
            String payload
    ) {
        return new OutboxEvent(aggregateId, aggregateType, eventType, payload);
    }

    public void markSent() {
        this.status = OutboxStatus.SENT;
    }

    public void markFailed() {
        this.status = OutboxStatus.FAILED;
    }
    public void incrementRetry(){
        this.retryCount ++;
    }
    public void scheduleNextRetry() {
        this.nextRetryAt = Instant.now()
                .plusSeconds(Math.min(60, retryCount * 5L));
    }

}