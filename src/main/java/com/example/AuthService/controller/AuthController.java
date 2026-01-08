package com.example.AuthService.controller;

import com.example.AuthService.dto.*;
import com.example.AuthService.dto.request.*;
import com.example.AuthService.dto.response.ApiResponse;
import com.example.AuthService.dto.response.TokenResponse;
import com.example.AuthService.service.AuthProfileService;
import com.example.AuthService.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthProfileService authProfileService;


    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest body) {
        return authService.login(
                body.getEmail(),
                body.getPassword(),
                body.getClientView()
        );
    }


    @PostMapping("/refresh")
    public TokenResponse refresh(@RequestBody Map<String, String> body) {
        return authService.refresh(body.get("refreshToken"));
    }

    // ---- Đăng ký OTP 2 bước ----
    @PostMapping("/register/start")
    @ResponseStatus(HttpStatus.OK) // trả 200 nếu thành công
    public void registerStart(@RequestBody RegisterStartRequest req) {
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

    // GET /auth/login/google -> chuyển sang flow OAuth2
    @GetMapping("/login/google")
    public ResponseEntity<Void> loginWithGoogle() {
        return ResponseEntity.status(302)
                .location(URI.create("/oauth2/authorization/google"))
                .build();
    }

    // Xem trạng thái đăng nhập hiện tại (JWT/OAuth2)
    @GetMapping("/me")
    public ResponseEntity<AuthProfileDto> me(@AuthenticationPrincipal Object principal) {
        return ResponseEntity.ok(authProfileService.buildProfile(principal, true));
    }
    @PostMapping("/otp/resend")
    public ResponseEntity<Void> resendOtp(@RequestBody OtpResendRequest req) {
        authService.resendOtp(req);
        return ResponseEntity.ok().build();
    }


}
