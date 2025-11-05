package com.example.AuthService.service;

import com.example.AuthService.entity.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface SocialLoginService {
    /**
     * Tạo mới hoặc cập nhật user khi đăng nhập bằng Google.
     * Ưu tiên map theo googleAccountId (sub); fallback theo email để hợp nhất tài khoản.
     */
    User upsertGoogleUser(OAuth2User oauth);
}
