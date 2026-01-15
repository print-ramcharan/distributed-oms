package com.oms.orderservice.infrastructure.idempotency;

import com.oms.orderservice.domain.idempotency.IdempotencyStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
@Getter
public class IdempotencyResult {

    private final boolean acquired;
    private final boolean completed;
    private final boolean inProgress;
    private final IdempotencyRecord existingRecord;

    public static IdempotencyResult acquired() {
        return new IdempotencyResult(true, false, false, null);
    }

    public static IdempotencyResult completed(IdempotencyRecord record) {
        return new IdempotencyResult(false, true, false, record);
    }

    public static IdempotencyResult inProgress() {
        return new IdempotencyResult(false, false, true, null);
    }

    public static IdempotencyResult from(IdempotencyRecord record) {
        if (record.getStatus() == IdempotencyStatus.COMPLETED) {
            return completed(record);
        }
        return inProgress();
    }

}
