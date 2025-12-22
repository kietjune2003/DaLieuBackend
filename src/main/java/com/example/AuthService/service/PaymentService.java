package com.example.AuthService.service;

import com.example.AuthService.entity.Payment;
import com.example.AuthService.entity.User;

import java.util.Map;

public interface PaymentService {

    String createVnPayPayment(Long orderId, User user);
    boolean handleVnpayReturn(Map<String, String> params);
    boolean handleVnpayIPN(Map<String, String> params);
}
