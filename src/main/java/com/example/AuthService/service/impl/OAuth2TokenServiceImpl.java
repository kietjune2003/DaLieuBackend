package com.example.AuthService.service.impl;

import com.example.AuthService.dto.response.AuthTokenResponse;
import com.example.AuthService.security.jwt.JwtProperties;
import com.example.AuthService.security.jwt.JwtService;
import com.example.AuthService.service.OAuth2TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OAuth2TokenServiceImpl implements OAuth2TokenService {

    private final JwtService jwtService;
    private final JwtProperties jwtProps; // để lấy accessExpirationMs

    @Override
    public AuthTokenResponse buildTokenResponse(UserDetails user, Map<String, Object> attributes) {
        String access  = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user.getUsername());

        Map<String, Object> a = attributes == null ? Collections.emptyMap() : attributes;
        List<String> roles = user.getAuthorities() == null
                ? List.of()
                : user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        return AuthTokenResponse.builder()
                .tokenType("Bearer")
                .accessToken(access)
                .refreshToken(refresh)
                .expiresIn(jwtProps.getAccessExpirationMs())
                .provider("google")
                .email(asString(a.get("email"), user.getUsername()))
                .name(asString(a.get("name"), null))
                .picture(asString(a.get("picture"), null))
                .sub(asString(a.get("sub"), null))
                .authorities(roles)
                .build();
    }

    private String asString(Object v, String fallback) {
        return v == null ? fallback : String.valueOf(v);
    }
}
