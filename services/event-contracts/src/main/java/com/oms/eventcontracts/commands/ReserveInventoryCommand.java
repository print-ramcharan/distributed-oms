package com.oms.eventcontracts.commands;

import java.time.Instant;
import java.util.List;

public class ReserveInventoryCommand {

    private String orderId;
    private List<LineItem> items;
    private Instant requestedAt;

    
    public ReserveInventoryCommand() {
    }

    
    public ReserveInventoryCommand(String orderId, List<LineItem> items, Instant requestedAt) {
        this.orderId = orderId;
        this.items = items;
        this.requestedAt = requestedAt;
    }

    
    public String getOrderId() {
        return orderId;
    }

    public List<LineItem> getItems() {
        return items;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    
    
    
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