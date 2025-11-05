package com.example.AuthService.security;

import com.example.AuthService.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Map;

@RequiredArgsConstructor
public class OAuth2UserPrincipal implements OAuth2User, UserDetails {

    private final User user;                      // User trong DB của bạn (implements UserDetails)
    private final Map<String, Object> attributes; // attributes từ Google (name, email, picture, sub...)

    // ===== OAuth2User =====
    @Override public Map<String, Object> getAttributes() { return attributes; }
    @Override public String getName() {
        // ưu tiên google sub, fallback id của user
        Object sub = attributes != null ? attributes.get("sub") : null;
        return sub != null ? sub.toString() : (user.getId() != null ? user.getId().toString() : user.getEmail());
    }

    // ===== UserDetails (uỷ quyền về entity User của bạn) =====
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return user.getAuthorities(); }
    @Override public String getPassword() { return user.getPassword(); }
    @Override public String getUsername() { return user.getUsername(); }
    @Override public boolean isAccountNonExpired() { return user.isAccountNonExpired(); }
    @Override public boolean isAccountNonLocked() { return user.isAccountNonLocked(); }
    @Override public boolean isCredentialsNonExpired() { return user.isCredentialsNonExpired(); }
    @Override public boolean isEnabled() { return user.isEnabled(); }

    // tiện dùng ở chỗ khác nếu cần lấy entity
    public User getUser() { return user; }
}
