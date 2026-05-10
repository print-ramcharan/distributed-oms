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
                        SELECT o.order_id, o.status, o.created_at, 
                               (SELECT product_id FROM order_items WHERE order_id = o.order_id LIMIT 1) as product_id
                        FROM orders o
                        ORDER BY o.created_at DESC
                        LIMIT ?
                        """,
                (rs, i) -> new OrderSummaryResponse(
                        UUID.fromString(rs.getString("order_id")),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getString("product_id")),
                limit);
    }

    public List<OrderSummaryResponse> findByCustomerEmail(String email) {
        return jdbcTemplate.query(
                """
                        SELECT o.order_id, o.status, o.created_at,
                               (SELECT product_id FROM order_items WHERE order_id = o.order_id LIMIT 1) as product_id
                        FROM orders o
                        WHERE o.customer_email = ?
                        ORDER BY o.created_at DESC
                        """,
                (rs, i) -> new OrderSummaryResponse(
                        UUID.fromString(rs.getString("order_id")),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getString("product_id")),
                email);
    }
}
