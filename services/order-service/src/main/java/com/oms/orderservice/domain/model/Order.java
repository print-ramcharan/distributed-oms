package com.oms.orderservice.domain.model;

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

    /* ========= Saga-controlled transitions ========= */

    public void markCompleted() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be completed");
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void markFailed(String reason) {
        if (this.status == OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot fail a confirmed order");
        }
        this.status = OrderStatus.FAILED;
        // Optional: persist failure reason in a column later
    }
}


//package com.oms.orderservice.domain.model;
//
//import jakarta.persistence.*;
//import lombok.Getter;
//
//import java.math.BigDecimal;
//import java.time.Instant;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//
//@Entity
//@Table(name = "orders")
//@Getter
//public class Order {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
//    @Column(name = "order_id", updatable = false, nullable = false)
//    private UUID id;
//
//    @OneToMany(
//            mappedBy = "order",
//            cascade = CascadeType.ALL,
//            orphanRemoval = true
//    )
//    private List<OrderItem> items = new ArrayList<>();
//
//    @Column(name = "total_amount", nullable = false)
//    private BigDecimal totalAmount;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private OrderStatus status;
//
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private Instant createdAt;
//
//    protected Order() {
//        // JPA only
//    }
//
//    /* ========= Factory ========= */
//
//    public static Order create(List<OrderItem> rawItems) {
//        if (rawItems == null || rawItems.isEmpty()) {
//            throw new IllegalArgumentException("Order must contain at least one item");
//        }
//
//        Order order = new Order();
//        order.createdAt = Instant.now();
//        order.status = OrderStatus.PENDING;
//
//        for (OrderItem item : rawItems) {
//            order.addItem(item);
//        }
//
//        order.recalculateTotal();
//        return order;
//    }
//
//    /* ========= Domain behavior ========= */
//
//    private void addItem(OrderItem item) {
//        item.attachTo(this);   // enforce invariant
//        this.items.add(item);
//    }
//
//    private void recalculateTotal() {
//        this.totalAmount = items.stream()
//                .map(OrderItem::totalPrice)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//    }
//
//    private void markCompleted(){
//        this.status = OrderStatus.CONFIRMED;
//
//    }
//    private void markFailed(String reason){
//        this.status = OrderStatus.FAILED;
//
//    }
//}
