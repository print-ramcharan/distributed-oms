package com.oms.orderservice.domain.model;

public enum OrderProgress {
    ORDER_ACCEPTED,
    AWAITING_PAYMENT,
    AWAITING_STOCK_CONFIRMATION,
    PREPARING_FOR_FULFILLMENT,
    ORDER_COMPLETED,
    ORDER_FAILED
}


