package com.example.AuthService.service;

import com.example.AuthService.dto.request.CreateOrderRequest;
import com.example.AuthService.dto.response.BulkOrderActionResult;
import com.example.AuthService.dto.response.OrderResponse;
import com.example.AuthService.dto.response.PageResponse;
import com.example.AuthService.entity.Order;
import com.example.AuthService.entity.User;
import com.example.AuthService.enums.OrderStatus;
import com.example.AuthService.enums.PaymentMethod;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.nio.file.AccessDeniedException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {
    Order createOrder(CreateOrderRequest request, User user);
    Order confirmCodPayment(Long orderId, User user);

//    @Transactional
//    void updateOrderStatus(Long orderId, OrderStatus newStatus, User user);

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
    OrderResponse getOrderById(User user, Long orderId) throws AccessDeniedException;

    @Transactional
    void approveRefund(Long orderId, User admin);

    @Transactional
    void rejectRefund(Long orderId, User admin);



    Page<OrderResponse> getOrdersForAdmin(
            OrderStatus status,
            PaymentMethod paymentMethod,
            Long userId,
            String keyword,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDir,
            int page,
            int size
    );

    OrderResponse getOrderDetailForAdmin(Long orderId);

    @Transactional
    BulkOrderActionResult bulkShipOrders(List<Long> orderIds);

    @Transactional
    BulkOrderActionResult bulkCompleteOrders(List<Long> orderIds);
    BulkOrderActionResult bulkCancelOrders(
            List<Long> orderIds,
            User admin
    );


    BulkOrderActionResult bulkApproveRefund(List<Long> orderIds, User admin);

    BulkOrderActionResult bulkRejectRefund(List<Long> orderId, User admin);
}
