package com.oms.orderservice.domain.repository;

import com.oms.orderservice.api.dto.OrderSummaryResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class OrderQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrderQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<OrderSummaryResponse> findRecent(int limit) {
        return jdbcTemplate.query(
                """
                SELECT order_id, status, created_at
                FROM orders
                ORDER BY created_at DESC
                LIMIT ?
                """,
                (rs, i) -> new OrderSummaryResponse(
                        UUID.fromString(rs.getString("order_id")),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                limit
        );
    }
}

