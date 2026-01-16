package com.oms.orderservice.domain.lifecycle;

import com.oms.eventcontracts.enums.OrderProgress;

import java.util.Map;
import java.util.Set;

public final class OrderProgressTransitions {

    private static final Map<OrderProgress, Set<OrderProgress>> COMMAND_ALLOWED = Map.of(
            OrderProgress.ORDER_ACCEPTED,
            Set.of(OrderProgress.AWAITING_PAYMENT),

            OrderProgress.AWAITING_PAYMENT,
            Set.of(OrderProgress.AWAITING_STOCK_CONFIRMATION),

            OrderProgress.AWAITING_STOCK_CONFIRMATION,
            Set.of(OrderProgress.PREPARING_FOR_FULFILLMENT),

            OrderProgress.PREPARING_FOR_FULFILLMENT,
            Set.of()
    );

    private static final Map<OrderProgress, Set<OrderProgress>> INTERNAL_DERIVED = Map.of(
            OrderProgress.PREPARING_FOR_FULFILLMENT,
            Set.of(OrderProgress.ORDER_COMPLETED),

            OrderProgress.ORDER_ACCEPTED,
            Set.of(OrderProgress.ORDER_FAILED),

            OrderProgress.AWAITING_PAYMENT,
            Set.of(OrderProgress.ORDER_FAILED),

            OrderProgress.AWAITING_STOCK_CONFIRMATION,
            Set.of(OrderProgress.ORDER_FAILED)
    );

    private OrderProgressTransitions() {}

    public static boolean isCommandAllowed(OrderProgress current, OrderProgress next) {
        return COMMAND_ALLOWED
                .getOrDefault(current, Set.of())
                .contains(next);
    }

    public static Set<OrderProgress> derivedFrom(OrderProgress current) {
        return INTERNAL_DERIVED.getOrDefault(current, Set.of());
    }
}
