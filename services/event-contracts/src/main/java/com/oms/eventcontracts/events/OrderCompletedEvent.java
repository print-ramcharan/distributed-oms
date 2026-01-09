package com.oms.eventcontracts.events;

import java.util.UUID;

public record OrderCompletedEvent(UUID orderId, String completedAt) {}
//public class OrderCompletedEvent {
//
//    private UUID orderId;
//    private String completedAt; // 👈 Changed from Instant to String
//
//    public OrderCompletedEvent() {
//    }
//
//    public OrderCompletedEvent(UUID orderId, String completedAt) {
//        this.orderId = orderId;
//        this.completedAt = completedAt;
//    }
//
//    public UUID getOrderId() {
//        return orderId;
//    }
//
//    public String getCompletedAt() {
//        return completedAt;
//    }
//}