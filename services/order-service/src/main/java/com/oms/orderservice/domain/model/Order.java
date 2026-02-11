package com.oms.orderservice.domain.model;

import com.oms.eventcontracts.enums.OrderProgress;
import com.oms.orderservice.domain.lifecycle.OrderProgressTransitions;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.io.Serializable;

@Entity
@Table(name = "orders")
@Getter
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Order implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", updatable = false, nullable = false)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "progress", nullable = false)
    private OrderProgress progress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    protected Order() {
    }

    /* ========= Factory ========= */

    public static Order create(List<OrderItem> rawItems, String customerEmail) {
        if (rawItems == null || rawItems.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        if (customerEmail == null || customerEmail.isBlank()) {
            throw new IllegalArgumentException("Customer email must be provided");
        }

        Order order = new Order();
        order.customerEmail = customerEmail;
        order.createdAt = Instant.now();
        order.status = OrderStatus.PENDING;
        order.progress = OrderProgress.ORDER_ACCEPTED;
        for (OrderItem item : rawItems) {
            order.addItem(item);
        }

        order.recalculateTotal();
        return order;
    }

    /* ========= Domain behavior ========= */

    private void addItem(OrderItem item) {
        item.attachTo(this);
        this.items.add(item);
    }

    private void recalculateTotal() {
        this.totalAmount = items.stream()
                .map(OrderItem::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void advanceProgress(OrderProgress next) {

        // 1️⃣ Idempotency
        if (this.progress == next) {
            return;
        }

        // 2️⃣ Terminal guard
        if (this.progress == OrderProgress.ORDER_COMPLETED
                || this.progress == OrderProgress.ORDER_FAILED) {
            throw new IllegalStateException("Order is in terminal state");
        }

        // 3️⃣ Validate transition
        if (!OrderProgressTransitions.isCommandAllowed(this.progress, next)) {
            throw new IllegalStateException(
                    "Illegal transition: " + this.progress + " → " + next);
        }

        // 4️⃣ Apply progress
        this.progress = next;

        // 5️⃣ Derive status (single authority)
        switch (next) {
            case ORDER_COMPLETED -> {
                this.status = OrderStatus.COMPLETED;
                this.completedAt = Instant.now();
            }
            case ORDER_FAILED -> {
                this.status = OrderStatus.CANCELLED;
                this.cancelledAt = Instant.now();
            }
            default -> this.status = OrderStatus.PENDING;
        }
    }

}
