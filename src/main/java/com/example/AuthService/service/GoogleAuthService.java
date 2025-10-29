package com.example.AuthService.service;

import com.example.AuthService.dto.GoogleLoginRequest;
import com.example.AuthService.dto.TokenResponse;

public interface GoogleAuthService {
    TokenResponse loginWithGoogle(GoogleLoginRequest req);
}
