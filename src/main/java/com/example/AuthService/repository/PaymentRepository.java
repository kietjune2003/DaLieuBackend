package com.example.AuthService.repository;

import com.example.AuthService.entity.OrderItem;
import com.example.AuthService.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByVnpTxnRef(String txnRef);

    Optional<Payment> findByOrderId(Long orderId);
}


