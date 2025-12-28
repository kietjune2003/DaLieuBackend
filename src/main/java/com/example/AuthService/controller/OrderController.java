package com.example.AuthService.controller;

import com.example.AuthService.dto.request.CreateOrderRequest;
import com.example.AuthService.entity.Order;
import com.example.AuthService.entity.User;
import com.example.AuthService.enums.OrderStatus;
import com.example.AuthService.enums.PaymentMethod;
import com.example.AuthService.repository.UserRepository;
import com.example.AuthService.service.OrderService;
import lombok.RequiredArgsConstructor;


import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    // ✅ 1. USER tạo order (JWT)
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createOrder(
            @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy user: " + email));

        Order order = orderService.createOrder(request, user);

        return ResponseEntity.ok(
                "✅ Tạo đơn hàng thành công! Order ID: " + order.getId()
        );
    }
    // ✅ 2. User xác nhận thanh toán COD
    @PostMapping("/{orderId}/confirm-cod")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> confirmCodPayment(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy user: " + email));

        Order order = orderService.confirmCodPayment(orderId, user);

        return ResponseEntity.ok(
                "✅ Đã xác nhận COD cho đơn hàng ID: " + order.getId()
        );
    }
    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        orderService.cancelOrder(orderId, user);

        return ResponseEntity.ok("Đã huỷ đơn hàng");
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getOrders(
            @AuthenticationPrincipal UserDetails userDetails,

            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentMethod paymentMethod,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate,

            @RequestParam(required = false) Long userId,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,

            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(
                orderService.getOrders(
                        user, status, paymentMethod,
                        fromDate, toDate, userId, pageable
                )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) throws AccessDeniedException {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        return ResponseEntity.ok(orderService.getOrderById(user, id));
    }

}
