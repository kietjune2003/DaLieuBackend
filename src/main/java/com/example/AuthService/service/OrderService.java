package com.example.AuthService.service;

import com.example.AuthService.dto.request.CreateOrderRequest;
import com.example.AuthService.entity.Order;
import com.example.AuthService.entity.User;
import com.example.AuthService.enums.OrderStatus;
import jakarta.transaction.Transactional;

public interface OrderService {
    Order createOrder(CreateOrderRequest request, User user);
    Order confirmCodPayment(Long orderId, User user);

    @Transactional
    void updateOrderStatus(Long orderId, OrderStatus newStatus, User user);
}
