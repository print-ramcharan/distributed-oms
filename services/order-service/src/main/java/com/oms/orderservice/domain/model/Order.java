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

@Entity
@Table(name = "orders")
@Getter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", updatable = false, nullable = false)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "progress", nullable = false)
    private OrderProgress progress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Order() {}

    /* ========= Factory ========= */

    public static Order create(List<OrderItem> rawItems) {
        if (rawItems == null || rawItems.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        Order order = new Order();
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

        if (this.progress == next) {
            return; // idempotent replay
        }

        if (!OrderProgressTransitions.isCommandAllowed(this.progress, next)) {
            throw new IllegalStateException(
                    "Illegal commanded transition: " + this.progress + " → " + next
            );
        }

        this.progress = next;

        // 🔐 DOMAIN-OWNED FACTS
        for (OrderProgress derived : OrderProgressTransitions.derivedFrom(this.progress)) {
            this.progress = derived;
        }
    }




}
