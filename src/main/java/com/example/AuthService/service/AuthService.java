package com.example.AuthService.service;

import com.example.AuthService.dto.*;
import com.example.AuthService.entity.User;

public interface AuthService {
    TokenResponse login(String email, String password);
    TokenResponse refresh(String refreshToken);

    void registerStart(RegisterStartRequest req);
    TokenResponse registerVerify(OtpVerifyRequest req);

    void forgotPassword(ForgotPasswordRequest req);
    void resetPassword(ResetPasswordRequest req);

    TokenResponse register(RegisterRequest req); // nếu vẫn muốn giữ

    User getByEmailOrThrow(String email);
}
