package com.oms.orderservice.infrastructure.idempotency;

import com.oms.orderservice.domain.idempotency.IdempotencyStatus;
import jakarta.persistence.Id;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${idempotency.order.ttl-hours")
    private long ttlHours;

    public IdempotencyResult tyrAcquire(String idempotencyKey){
        String redisKey = IdempotencyKeyUtil.orderCreate(idempotencyKey);

        IdempotencyRecord record = new IdempotencyRecord(IdempotencyStatus.IN_PROGRESS, null, null, Instant.now());
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(redisKey, record, Duration.ofHours(ttlHours));

        if(Boolean.TRUE.equals(acquired)){
            return IdempotencyResult.acquired();
        }
        throw new IllegalStateException("Invalid idempotency record in Redis");
    }

    public void markCompleted(String idempotencyKey, UUID orderId, String responseJson){
        String redisKey = IdempotencyKeyUtil.orderCreate(idempotencyKey);

        IdempotencyRecord completed = new IdempotencyRecord(IdempotencyStatus.COMPLETED, orderId, responseJson, Instant.now());

        redisTemplate.opsForValue().set(redisKey, completed, Duration.ofHours(ttlHours));

    }

    public void clear(String idempotencyKey){
        redisTemplate.delete(IdempotencyKeyUtil.orderCreate(idempotencyKey));
    }

}

