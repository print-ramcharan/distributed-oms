package com.oms.notificationservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String subject;

    private String type; // e.g., ORDER_CREATED

    private Instant sentAt;

    public Notification(UUID orderId, String recipient, String subject, String type) {
        this.orderId = orderId;
        this.recipient = recipient;
        this.subject = subject;
        this.type = type;
        this.sentAt = Instant.now();
    }
}
