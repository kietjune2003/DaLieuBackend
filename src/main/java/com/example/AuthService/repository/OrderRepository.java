package com.example.AuthService.repository;

import com.example.AuthService.entity.Order;
import com.example.AuthService.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    List<Order> findByUserId(Long userId);


    Optional<Order> findByIdAndUserId(Long id, Long userId);
    List<Order> findByStatus(OrderStatus status);

    @Query("""
        select o from Order o
        where (:status is null or o.status = :status)
    """)
    List<Order> filterOrders(@Param("status") OrderStatus status);
    @Query("""
    select distinct o from Order o
    left join fetch o.items i
    left join fetch i.drug
    left join fetch o.user
    where o.id = :orderId
""")
    Optional<Order> findDetailById(@Param("orderId") Long orderId);
    @Query("""
        select distinct o
        from Order o
        where o.status = 'PENDING'
          and o.paymentMethod is null
          and o.createdAt < :expiredTime
    """)
    List<Order> findExpiredUnconfirmedOrders(
            @Param("expiredTime") LocalDateTime expiredTime
    );

}