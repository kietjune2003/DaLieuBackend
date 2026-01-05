package com.example.AuthService.repository;

import com.example.AuthService.entity.Order;
import com.example.AuthService.entity.OrderItem;
import com.example.AuthService.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByVnpTxnRef(String txnRef);

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByOrder(Order order);
}


