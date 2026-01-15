package com.oms.orderservice.infrastructure.idempotency;

public final class IdempotencyKeyUtil {

    private static final String ORDER_CREATE_PREFIX = "idem:order:create:";

    private IdempotencyKeyUtil() {}

    public static String orderCreate(String idempotencyKey) {
        return ORDER_CREATE_PREFIX + idempotencyKey;
    }
}
