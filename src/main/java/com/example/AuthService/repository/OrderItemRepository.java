package com.example.AuthService.repository;

import com.example.AuthService.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("""
        SELECT COALESCE(SUM(oi.quantity), 0)
        FROM OrderItem oi
        WHERE oi.drug.id = :drugId
          AND oi.order.status = com.example.AuthService.enums.OrderStatus.COMPLETED
    """)
    Integer totalSold(Long drugId);
}
