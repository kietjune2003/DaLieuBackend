package com.example.AuthService.controller;

import com.example.AuthService.entity.User;
import com.example.AuthService.enums.OrderStatus;
import com.example.AuthService.repository.UserRepository;
import com.example.AuthService.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
public class AdminOrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;
    public AdminOrderController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @PutMapping("/{orderId}/status")
    @PreAuthorize("isAuthenticated()") // user hoặc admin
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        orderService.updateOrderStatus(orderId, status, user);
        return ResponseEntity.ok("Order cập nhật trạng thái: " + status);
    }

}
