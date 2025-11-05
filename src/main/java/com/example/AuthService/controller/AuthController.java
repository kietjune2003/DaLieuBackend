package com.example.AuthService.controller;

import com.example.AuthService.dto.*;
import com.example.AuthService.service.AuthService;

import com.example.AuthService.service.GoogleAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;

    @PostMapping("/login")
    public TokenResponse login(@RequestBody Map<String, String> body) {
        return authService.login(body.get("email"), body.get("password"));
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@RequestBody Map<String, String> body) {
        return authService.refresh(body.get("refreshToken"));
    }

    // ---- Đăng ký OTP 2 bước ----
    @PostMapping("/register/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerStart(@Valid @RequestBody RegisterStartRequest req) {
        authService.registerStart(req);
    }

    @PostMapping("/register/verify")
    public TokenResponse registerVerify(@Valid @RequestBody OtpVerifyRequest req) {
        return authService.registerVerify(req);
    }

    // ---- Quên mật khẩu / đặt lại bằng OTP ----
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
    }

    // ---- Google Sign-in ----
    @PostMapping("/google")
    public TokenResponse loginWithGoogle(@RequestBody GoogleLoginRequest req) {
        return googleAuthService.loginWithGoogle(req);
    }


}
