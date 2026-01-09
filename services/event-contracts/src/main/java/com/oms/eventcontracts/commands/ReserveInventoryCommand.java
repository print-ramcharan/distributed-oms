package com.oms.eventcontracts.commands;

import java.time.Instant;
import java.util.List;

public class ReserveInventoryCommand {

    private String orderId;
    private List<LineItem> items;
    private Instant requestedAt;

    // 1. Default Constructor
    public ReserveInventoryCommand() {
    }

    // 2. All-Args Constructor
    public ReserveInventoryCommand(String orderId, List<LineItem> items, Instant requestedAt) {
        this.orderId = orderId;
        this.items = items;
        this.requestedAt = requestedAt;
    }

    // 3. Getters
    public String getOrderId() {
        return orderId;
    }

    public List<LineItem> getItems() {
        return items;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    // =================================================================
    // Inner Static Class for the Items (Simple DTO)
    // =================================================================
    public static class LineItem {
        private String productId;
        private int quantity;

        public LineItem() {
        }

        public LineItem(String productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public String getProductId() {
            return productId;
        }

        public int getQuantity() {
            return quantity;
        }
    }
}