package com.example.AuthService.service.impl;

import com.example.AuthService.entity.Role;
import com.example.AuthService.entity.User;
import com.example.AuthService.repository.RoleRepository;
import com.example.AuthService.repository.UserRepository;
import com.example.AuthService.service.SocialLoginService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class SocialLoginServiceImpl implements SocialLoginService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;

    @Override
    public User upsertGoogleUser(OAuth2User oauth) {
        // Thuộc tính chuẩn từ Google OIDC
        String sub     = oauth.getAttribute("sub");      // Google unique id
        String email   = norm(oauth.getAttribute("email"));
        String name    = oauth.getAttribute("name");
        String picture = oauth.getAttribute("picture");

        if (!StringUtils.hasText(sub)) {
            throw new IllegalStateException("Google 'sub' (account id) is missing");
        }

        // 1) Tìm theo googleAccountId; 2) fallback theo email để hợp nhất tài khoản
        return userRepo.findByGoogleAccountId(sub)
                .or(() -> StringUtils.hasText(email) ? userRepo.findByEmail(email) : java.util.Optional.empty())
                .map(u -> {
                    u.setGoogleAccountId(sub);
                    if (StringUtils.hasText(email))   u.setEmail(email);
                    if (StringUtils.hasText(name))    u.setName(name);
                    if (StringUtils.hasText(picture)) u.setPhotoUrl(picture);
                    u.setEnabled(true);
                    return userRepo.save(u);
                })
                .orElseGet(() -> {
                    Role userRole = roleRepo.findByName("USER")
                            .orElseThrow(() -> new IllegalStateException("Default role 'USER' not found"));

                    User u = new User(sub, email, name, picture);
                    u.setRole(userRole);

                    return userRepo.save(u);
                });
    }

    private String norm(String raw) {
        return raw == null ? null : raw.trim().toLowerCase(Locale.ROOT);
    }
}
