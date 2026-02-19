package com.oms.fulfillmentservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a fulfillment task created when inventory is successfully
 * reserved.
 * Tracks the lifecycle: INITIATED → DISPATCHED → DELIVERED (or FAILED).
 */
@Entity
@Table(name = "fulfillment_tasks")
@Getter
@NoArgsConstructor
public class FulfillmentTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FulfillmentStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    public static FulfillmentTask create(String orderId) {
        FulfillmentTask t = new FulfillmentTask();
        t.orderId = orderId;
        t.status = FulfillmentStatus.INITIATED;
        t.createdAt = Instant.now();
        t.updatedAt = Instant.now();
        return t;
    }

    public void dispatch() {
        this.status = FulfillmentStatus.DISPATCHED;
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        this.status = FulfillmentStatus.FAILED;
        this.updatedAt = Instant.now();
    }
}
