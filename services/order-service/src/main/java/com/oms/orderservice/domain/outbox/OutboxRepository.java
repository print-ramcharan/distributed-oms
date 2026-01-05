package com.oms.orderservice.domain.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query(value = """
    SELECT *
    FROM outbox_events
    WHERE status = 'NEW'
    ORDER BY created_at
    LIMIT :limit
    FOR UPDATE SKIP LOCKED
""", nativeQuery = true)
    List<OutboxEvent> fetchPendingEvents(@Param("limit") int limit);
}
