package com.example.AuthService.service;

import com.example.AuthService.dto.request.ForgotPasswordRequest;
import com.example.AuthService.dto.request.OtpVerifyRequest;
import com.example.AuthService.dto.request.RegisterStartRequest;
import com.example.AuthService.dto.request.ResetPasswordRequest;
import com.example.AuthService.dto.response.ApiResponse;
import com.example.AuthService.dto.response.TokenResponse;
import com.example.AuthService.entity.User;

public interface AuthService {
    TokenResponse login(String email, String password);
    TokenResponse refresh(String refreshToken);

    void registerStart(RegisterStartRequest req);
    TokenResponse registerVerify(OtpVerifyRequest req);

    void forgotPassword(ForgotPasswordRequest req);
    void resetPassword(ResetPasswordRequest req);

    User getByEmailOrThrow(String email);
}
