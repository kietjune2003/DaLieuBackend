package com.example.AuthService.controller;

import com.example.AuthService.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/vnpay")
public class VnPayIPNController {

    private final PaymentService paymentService;

    public VnPayIPNController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

//    // VNPay gọi POST
//    @PostMapping("/ipn")
//    public ResponseEntity<String> receiveIPN(@RequestParam Map<String, String> params) {
//
//        boolean success = paymentService.handleVnpayIPN(params);
//
//        // VNPay yêu cầu trả về code
//        if (success) {
//            return ResponseEntity.ok("vnp_ResponseCode=00");
//        } else {
//            return ResponseEntity.ok("vnp_ResponseCode=97"); // thất bại
//        }
//    }
    @GetMapping("/ipn")
    public ResponseEntity<Map<String, String>> receiveIPN(
            @RequestParam Map<String, String> params
    ) {
        boolean success = paymentService.handleVnpayIPN(params);

        Map<String, String> response = new HashMap<>();

        if (success) {
            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
        } else {
            response.put("RspCode", "97");
            response.put("Message", "Confirm Failed");
        }

        return ResponseEntity.ok(response);
    }
}
