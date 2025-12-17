package com.example.AuthService.service;

import com.example.AuthService.dto.response.UserProfileResponse;

public interface UserService {
    UserProfileResponse getUserProfileByEmail(String email);
}
