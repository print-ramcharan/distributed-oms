package com.oms.orderservice.domain.lifecycle;

import com.oms.eventcontracts.enums.OrderProgress;

import java.util.Map;
import java.util.Set;

public final class OrderProgressTransitions {

    private static final Map<OrderProgress, Set<OrderProgress>> ALLOWED = Map.of(
            OrderProgress.ORDER_ACCEPTED,
            Set.of(OrderProgress.AWAITING_PAYMENT, OrderProgress.ORDER_FAILED),

            OrderProgress.AWAITING_PAYMENT,
            Set.of(OrderProgress.AWAITING_STOCK_CONFIRMATION, OrderProgress.ORDER_FAILED),

            OrderProgress.AWAITING_STOCK_CONFIRMATION,
            Set.of(OrderProgress.PREPARING_FOR_FULFILLMENT, OrderProgress.ORDER_FAILED),

            OrderProgress.PREPARING_FOR_FULFILLMENT,
            Set.of(OrderProgress.ORDER_COMPLETED, OrderProgress.ORDER_FAILED),

            OrderProgress.ORDER_COMPLETED,
            Set.of(),

            OrderProgress.ORDER_FAILED,
            Set.of()
    );

    public static boolean isValid(OrderProgress current, OrderProgress next) {
        return ALLOWED.getOrDefault(current, Set.of()).contains(next);
    }
}

