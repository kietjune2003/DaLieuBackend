package com.example.AuthService.controller;

import com.example.AuthService.entity.User;
import com.example.AuthService.repository.UserRepository;
import com.example.AuthService.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    // ✅ 3. User thanh toán VNPay
    @PostMapping("/vnpay/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createVnPayPayment(
            @PathVariable Long orderId,
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        String paymentUrl = paymentService.createVnPayPayment(orderId, user, request);

        return ResponseEntity.ok(Map.of(
                "paymentUrl", paymentUrl
        ));
    }
    @GetMapping("/vnpay/return")
    public ResponseEntity<?> vnpayReturn(@RequestParam Map<String, String> params) {

        boolean success = paymentService.handleVnpayReturn(params);

        if (success) {
            return ResponseEntity.ok("✅ Thanh toán thành công");
        }
        return ResponseEntity.badRequest().body("❌ Thanh toán thất bại");
    }
}

