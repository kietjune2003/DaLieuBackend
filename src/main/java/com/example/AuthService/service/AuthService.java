package com.example.AuthService.service;

import com.example.AuthService.dto.request.*;
import com.example.AuthService.dto.response.ApiResponse;
import com.example.AuthService.dto.response.TokenResponse;
import com.example.AuthService.entity.User;

public interface AuthService {


    TokenResponse login(String emailRaw, String password, String clientView);

    TokenResponse refresh(String refreshToken);

    void registerStart(RegisterStartRequest req);
    TokenResponse registerVerify(OtpVerifyRequest req);

    void forgotPassword(ForgotPasswordRequest req);
    void resetPassword(ResetPasswordRequest req);
    void resendOtp(OtpResendRequest req);
    User getByEmailOrThrow(String email);
}
