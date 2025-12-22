package com.example.AuthService.controller;

import com.example.AuthService.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/vnpay")
public class VnPayIPNController {

    private final PaymentService paymentService;

    public VnPayIPNController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // VNPay gọi POST
    @PostMapping("/ipn")
    public ResponseEntity<String> receiveIPN(@RequestParam Map<String, String> params) {

        boolean success = paymentService.handleVnpayIPN(params);

        // VNPay yêu cầu trả về code
        if (success) {
            return ResponseEntity.ok("vnp_ResponseCode=00");
        } else {
            return ResponseEntity.ok("vnp_ResponseCode=97"); // thất bại
        }
    }
}
