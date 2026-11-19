package com.example.demo.repository;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository
        extends JpaRepository<Order, String> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Integer userId);

    java.util.Optional<Order> findByOrderIdAndUserId(
            String orderId, Integer userId);

    List<Order> findByStatus(OrderStatus status);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            LocalDateTime from, LocalDateTime to);

    long countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            OrderStatus status,
            LocalDateTime from,
            LocalDateTime to);

    @Query("""
        SELECT COALESCE(SUM(o.totalAmount), 0)
        FROM Order o
        WHERE o.status = :status
          AND o.createdAt >= :from
          AND o.createdAt < :to
        """)
    BigDecimal getRevenue(
            @Param("status") OrderStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(value = """
        SELECT DATE(o.created_at) AS sale_date,
               COUNT(o.order_id) AS order_count,
               COALESCE(SUM(o.total_amount), 0) AS revenue
        FROM orders o
        WHERE o.status = 'SUCCESS'
          AND o.created_at >= :from
          AND o.created_at < :to
        GROUP BY DATE(o.created_at)
        ORDER BY sale_date
        """, nativeQuery = true)
    List<Object[]> getDailySales(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(value = """
        SELECT oi.product_id,
               oi.product_name,
               SUM(oi.quantity) AS quantity_sold,
               SUM(oi.total_price) AS revenue
        FROM order_items oi
        JOIN orders o ON o.order_id = oi.order_id
        WHERE o.status = 'SUCCESS'
          AND o.created_at >= :from
          AND o.created_at < :to
        GROUP BY oi.product_id, oi.product_name
        ORDER BY quantity_sold DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> getTopProducts(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("limit") int limit);
}