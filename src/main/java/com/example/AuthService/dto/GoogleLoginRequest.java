package com.example.AuthService.dto;

import lombok.Data;

@Data
public class GoogleLoginRequest {
    private String idToken; // id_token lấy từ Google Identity Services
}
