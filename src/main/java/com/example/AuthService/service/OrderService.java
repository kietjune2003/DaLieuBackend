package com.example.AuthService.service;

import com.example.AuthService.dto.request.CreateOrderRequest;
import com.example.AuthService.dto.response.OrderResponse;
import com.example.AuthService.dto.response.PageResponse;
import com.example.AuthService.entity.Order;
import com.example.AuthService.entity.User;
import com.example.AuthService.enums.OrderStatus;
import com.example.AuthService.enums.PaymentMethod;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;


import java.time.LocalDateTime;

public interface OrderService {
    Order createOrder(CreateOrderRequest request, User user);
    Order confirmCodPayment(Long orderId, User user);

    @Transactional
    void updateOrderStatus(Long orderId, OrderStatus newStatus, User user);

    @Transactional
    void cancelOrder(Long orderId, User user);
    PageResponse<OrderResponse> getOrders(
            User user,
            OrderStatus status,
            PaymentMethod paymentMethod,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Long userId,
            Pageable pageable
    );
}
