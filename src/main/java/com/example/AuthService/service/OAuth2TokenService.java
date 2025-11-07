package com.example.AuthService.service;

import com.example.AuthService.dto.response.AuthTokenResponse;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;

public interface OAuth2TokenService {

    /**
     * Tạo AuthTokenResponse (access/refresh token + user info) từ UserDetails và các thuộc tính OAuth2.
     * @param user       user đã được xác thực (UserDetails trong hệ thống của bạn)
     * @param attributes thuộc tính từ provider (email, name, picture, sub,...), có thể null
     */
    AuthTokenResponse buildTokenResponse(UserDetails user, Map<String, Object> attributes);
}
