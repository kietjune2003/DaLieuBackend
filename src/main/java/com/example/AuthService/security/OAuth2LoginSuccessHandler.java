// com.example.AuthService.security.OAuth2LoginSuccessHandler
package com.example.AuthService.security;

import com.example.AuthService.dto.response.AuthTokenResponse;
import com.example.AuthService.entity.User;
import com.example.AuthService.service.OAuth2TokenService;
import com.example.AuthService.service.SocialLoginService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2TokenService tokenService;
    private final SocialLoginService socialLoginService;   // <--- THÊM
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        Object principal = authentication.getPrincipal();

        UserDetails userDetails;
        Map<String, Object> attributes;

        if (principal instanceof OAuth2UserPrincipal p) {
            userDetails = p; // đã là UserDetails (entity) + attributes
            attributes = p.getAttributes() != null ? p.getAttributes() : Collections.emptyMap();

        } else if (principal instanceof OAuth2User o) {
            // fallback an toàn: upsert + lấy User (UserDetails) từ DB
            User saved = socialLoginService.upsertGoogleUser(o);
            userDetails = saved; // User implements UserDetails
            attributes = o.getAttributes() != null ? o.getAttributes() : Collections.emptyMap();

        } else if (principal instanceof UserDetails u) {
            userDetails = u;
            attributes = Collections.emptyMap();

        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Unsupported principal type\"}");
            return;
        }

        AuthTokenResponse payload = tokenService.buildTokenResponse(userDetails, attributes);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), payload);
    }
}
