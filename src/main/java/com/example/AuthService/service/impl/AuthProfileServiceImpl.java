package com.example.AuthService.service.impl;

import com.example.AuthService.dto.AuthProfileDto;
import com.example.AuthService.security.OAuth2UserPrincipal;
import com.example.AuthService.service.AuthProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthProfileServiceImpl implements AuthProfileService {

    @Override
    public AuthProfileDto buildProfile(Object principal, boolean includeAuthorities) {
        if (principal == null) {
            return AuthProfileDto.builder()
                    .authenticated(false)
                    .build();
        }

        if (principal instanceof OAuth2UserPrincipal p) {
            Map<String, Object> a = safeAttrs(p.getAttributes());
            return AuthProfileDto.builder()
                    .authenticated(true)
                    .provider("google")
                    .email(asString(a.get("email"), p.getUsername()))
                    .name(asString(a.get("name"), null))
                    .picture(asString(a.get("picture"), null))
                    .sub(asString(a.get("sub"), null))
                    .authorities(includeAuthorities ? toRoleList(p.getAuthorities()) : null)
                    .build();
        }

        if (principal instanceof OAuth2User o) {
            Map<String, Object> a = safeAttrs(o.getAttributes());
            return AuthProfileDto.builder()
                    .authenticated(true)
                    .provider("google")
                    .email(asString(a.get("email"), null))
                    .name(asString(a.get("name"), null))
                    .picture(asString(a.get("picture"), null))
                    .sub(asString(a.get("sub"), null))
                    .authorities(includeAuthorities ? toRoleList(o.getAuthorities()) : null)
                    .build();
        }

        if (principal instanceof UserDetails u) {
            return AuthProfileDto.builder()
                    .authenticated(true)
                    .provider("local/jwt")
                    .email(u.getUsername())
                    .name(u.getUsername())
                    .authorities(includeAuthorities ? toRoleList(u.getAuthorities()) : null)
                    .build();
        }

        return AuthProfileDto.builder()
                .authenticated(true)
                .provider(principal.getClass().getSimpleName())
                .build();
    }

    // -------- helpers --------
    private Map<String, Object> safeAttrs(Map<String, Object> attrs) {
        return attrs == null ? Collections.emptyMap() : attrs;
    }

    private String asString(Object v, String fallback) {
        return v == null ? fallback : String.valueOf(v);
    }

    private List<String> toRoleList(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null) return List.of();
        return authorities.stream().map(GrantedAuthority::getAuthority).toList();
    }
}
