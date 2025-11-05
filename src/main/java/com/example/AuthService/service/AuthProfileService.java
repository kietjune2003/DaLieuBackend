package com.example.AuthService.service;

import com.example.AuthService.dto.AuthProfileDto;

public interface AuthProfileService {
    /**
     * Xây dựng hồ sơ người dùng từ principal hiện tại (OAuth2/JWT).
     * @param principal đối tượng @AuthenticationPrincipal
     * @param includeAuthorities có trả kèm roles hay không
     */
    AuthProfileDto buildProfile(Object principal, boolean includeAuthorities);
}
