package com.oms.orderservice.application.outbox;

import org.apache.kafka.common.errors.TimeoutException;

public final class PublishFailureClassifier {
    private PublishFailureClassifier() {}

    public static PublishFailureType classify(Exception ex) {

        if (ex.getCause() instanceof TimeoutException) {
            return PublishFailureType.TRANSIENT;
        }

        if (ex instanceof IllegalStateException) {
            return PublishFailureType.PERMANENT;
        }

        return PublishFailureType.TRANSIENT;
    }
}
