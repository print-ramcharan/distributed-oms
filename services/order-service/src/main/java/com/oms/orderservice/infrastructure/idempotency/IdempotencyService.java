package com.oms.orderservice.infrastructure.idempotency;

import com.oms.orderservice.domain.idempotency.IdempotencyStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;

    
    
    private static final Duration IN_PROGRESS_TTL = Duration.ofSeconds(60);
    private static final Duration COMPLETED_TTL = Duration.ofHours(24);

    public IdempotencyResult tryAcquire(String idempotencyKey) {
        String redisKey = IdempotencyKeyUtil.orderCreate(idempotencyKey);

        IdempotencyRecord newRecord = new IdempotencyRecord(
                IdempotencyStatus.IN_PROGRESS,
                null,
                null,
                Instant.now());

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, newRecord, IN_PROGRESS_TTL);

        if (Boolean.TRUE.equals(acquired)) {
            return IdempotencyResult.acquired();
        }

        Object existing = redisTemplate.opsForValue().get(redisKey);

        if (existing instanceof IdempotencyRecord existingRecord) {
            return IdempotencyResult.from(existingRecord);
        }

        throw new IllegalStateException("Corrupted idempotency record in Redis");
    }

    public void markCompleted(String idempotencyKey, UUID orderId, String responseJson) {
        String redisKey = IdempotencyKeyUtil.orderCreate(idempotencyKey);

        IdempotencyRecord completed = new IdempotencyRecord(IdempotencyStatus.COMPLETED, orderId, responseJson,
                Instant.now());

        redisTemplate.opsForValue()
                .set(redisKey, completed, COMPLETED_TTL);

    }

    public void clear(String idempotencyKey) {
        redisTemplate.delete(IdempotencyKeyUtil.orderCreate(idempotencyKey));
    }

}
