package com.example.AuthService.service.impl;

import com.example.AuthService.dto.GoogleLoginRequest;
import com.example.AuthService.dto.TokenResponse;
import com.example.AuthService.entity.Role;
import com.example.AuthService.entity.User;
import com.example.AuthService.repository.RoleRepository;
import com.example.AuthService.repository.UserRepository;
import com.example.AuthService.security.google.GoogleIdTokenVerifierService;
import com.example.AuthService.security.jwt.JwtService;
import com.example.AuthService.service.GoogleAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class GoogleAuthServiceImpl implements GoogleAuthService {

    private final GoogleIdTokenVerifierService googleVerifier;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final JwtService jwt;

    @Override
    public TokenResponse loginWithGoogle(GoogleLoginRequest req) {
        if (req.getIdToken() == null || req.getIdToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing idToken");
        }

        var payload = safeVerify(req.getIdToken());
        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google id_token");
        }

        String googleSub   = payload.getSubject();
        String email       = ((String) payload.getEmail()).toLowerCase(Locale.ROOT);
        boolean emailVerif = Boolean.TRUE.equals(payload.getEmailVerified());
        String name        = (String) payload.get("name");
        String picture     = (String) payload.get("picture");

        if (!emailVerif) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email is not verified by Google");
        }

        User user = userRepo.findByGoogleAccountId(googleSub)
                .orElseGet(() -> userRepo.findByEmail(email).orElse(null));

        if (user == null) {
            Role userRole = roleRepo.findByName("USER")
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Missing role USER"));

            user = User.builder()
                    .email(email)
                    .name(name != null ? name : email)
                    .password(null)
                    .googleAccountId(googleSub)
                    .photoUrl(picture)
                    .role(userRole)
                    .enabled(true)
                    .build();
        } else {
            if (user.getGoogleAccountId() == null) user.setGoogleAccountId(googleSub);
            if (name != null && (user.getName() == null || user.getName().isBlank())) user.setName(name);
            if (picture != null && (user.getPhotoUrl() == null || user.getPhotoUrl().isBlank())) user.setPhotoUrl(picture);
        }

        user = userRepo.save(user);
        return new TokenResponse(jwt.generateAccessToken(user), jwt.generateRefreshToken(user.getUsername()));
    }

    private com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload safeVerify(String idToken) {
        try { return googleVerifier.verify(idToken); }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google verification error"); }
    }
}
