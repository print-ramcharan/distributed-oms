package com.oms.orderservice.infrastructure.idempotency;

import com.oms.orderservice.domain.idempotency.IdempotencyStatus;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;


@AllArgsConstructor
public class IdempotencyResult {

    private final boolean acquired;
    private final IdempotencyRecord existingRecord;

    public static IdempotencyResult acquired(){
        return new IdempotencyResult(true, null);
    }

    public static IdempotencyResult existing(IdempotencyRecord record){
        return new IdempotencyResult(false, record);
    }

    public boolean isCompleted(){
        return existingRecord != null && existingRecord.getStatus() == IdempotencyStatus.COMPLETED;
    }

    public boolean isInProgress(){
        return existingRecord != null && existingRecord.getStatus() == IdempotencyStatus.IN_PROGRESS;
    }
}
