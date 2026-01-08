package com.example.AuthService.service.impl;

import com.example.AuthService.dto.request.*;
import com.example.AuthService.dto.response.TokenResponse;
import com.example.AuthService.entity.Country;
import com.example.AuthService.entity.Role;
import com.example.AuthService.entity.User;
import com.example.AuthService.otp.OtpService;
import com.example.AuthService.otp.OtpType;
import com.example.AuthService.repository.CountryRepository;
import com.example.AuthService.repository.RoleRepository;
import com.example.AuthService.repository.UserRepository;
import com.example.AuthService.security.jwt.JwtService;
import com.example.AuthService.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authManager;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final CountryRepository countryRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    private final OtpService otpService;
    private final ObjectMapper objectMapper;

    @Override
    public TokenResponse login(String emailRaw, String password, String clientView) {
        String email = normalize(emailRaw);

        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials");
        } catch (DisabledException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User disabled");
        } catch (LockedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User locked");
        }

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if ("ADMIN".equals(clientView)) {
            boolean isAdmin = user.getRole().
                    getName().equals("ADMIN");

            if (!isAdmin) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Only ADMIN can login to admin view"
                );
            }
        }

        return new TokenResponse(
                jwt.generateAccessToken(user),
                jwt.generateRefreshToken(user.getUsername())
        );
    }


    @Override
    public TokenResponse refresh(String refreshToken) {
        if (refreshToken == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing refreshToken");
        if (!jwt.isValid(refreshToken)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");

        String username = jwt.extractUsername(refreshToken);
        User user = userRepo.findByEmail(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        String newAccess = jwt.generateAccessToken(user);
        return new TokenResponse(newAccess, refreshToken); // giữ refresh cũ
    }

    // ===== Register OTP 2 bước =====
    @Override
    public void registerStart(RegisterStartRequest req) {
        String email = normalize(req.getEmail());

        if (userRepo.existsByEmail(email)) {

            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email Used");
        }

        // Chuẩn bị dữ liệu gửi kèm OTP
        Map<String, Object> payload = new HashMap<>();
        payload.put("passwordHash", passwordEncoder.encode(req.getPassword()));
        payload.put("name",        req.getName());
        payload.put("gender",      req.getGender());
        payload.put("phoneNumber", req.getPhoneNumber());
        payload.put("dateOfBirth", req.getDateOfBirth() != null ? req.getDateOfBirth().toString() : null);
        payload.put("countryId",   req.getCountryId());
        payload.put("photoUrl",    req.getPhotoUrl());

        // Gửi mã OTP
        otpService.sendOtp(email, OtpType.REGISTER, Optional.of(payload));


    }



    @Override
    public TokenResponse registerVerify(OtpVerifyRequest req) {
        String email = normalize(req.getEmail());
        var otp = otpService.verify(email, OtpType.REGISTER, req.getCode());

        Map<?,?> payload;
        try { payload = objectMapper.readValue(otp.getPayloadJson(), Map.class); }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid OTP payload"); }

        if (userRepo.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email Used");
        }

        Role userRole = roleRepo.findByName("USER")
                .orElseGet(() -> roleRepo.save(Role.builder().name("USER").build()));

        Country country = null;
        Object countryIdObj = payload.get("countryId");
        if (countryIdObj != null) {
            Long countryId = parseLong(countryIdObj);
            if (countryId != null) {
                country = countryRepo.findById(countryId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Country không tồn tại"));
            }
        }

        String name = (String) payload.get("name");
        String gender = (String) payload.get("gender");
        String phoneNumber = (String) payload.get("phoneNumber");
        String dobStr = (String) payload.get("dateOfBirth");
        LocalDate dob = (dobStr != null && !dobStr.isBlank()) ? LocalDate.parse(dobStr) : null;
        String photoUrl = (String) payload.get("photoUrl");
        String passwordHash = (String) payload.get("passwordHash");

        User user = User.builder()
                .email(email).name(name).password(passwordHash)
                .gender(gender).phoneNumber(phoneNumber).dateOfBirth(dob)
                .photoUrl(photoUrl).country(country).role(userRole)
                .enabled(true).accountNonExpired(true).accountNonLocked(true).credentialsNonExpired(true)
                .build();

        user = userRepo.save(user);
        return new TokenResponse(jwt.generateAccessToken(user), jwt.generateRefreshToken(user.getUsername()));
    }

    // ===== Quên / đặt lại mật khẩu =====
    @Override
    public void forgotPassword(ForgotPasswordRequest req) {
        String email = normalize(req.getEmail());
        if (!userRepo.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Email not found");
        }
        otpService.sendOtp(email, OtpType.RESET_PASSWORD, Optional.empty());
    }

    @Override
    public void resetPassword(ResetPasswordRequest req) {
        String email = normalize(req.getEmail());
        otpService.verify(email, OtpType.RESET_PASSWORD, req.getCode());
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepo.save(user);
    }


    @Override
    public User getByEmailOrThrow(String emailRaw) {
        return userRepo.findByEmail(normalize(emailRaw))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
    private Long parseLong(Object obj) {
        if (obj instanceof Number n) return n.longValue();
        if (obj instanceof String s && !s.isBlank()) return Long.valueOf(s);
        return null;
    }
    @Override
    public void resendOtp(OtpResendRequest req) {
        String email = normalize(req.getEmail());
        OtpType type = req.getType();

        if (type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OtpType không được để trống");
        }

        switch (type) {
            case REGISTER -> {
                if (userRepo.existsByEmail(email)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được sử dụng");
                }
                // Gửi lại OTP cho đăng ký (payload có thể để trống, vì chỉ cần xác nhận email)
                otpService.sendOtp(email, OtpType.REGISTER, Optional.empty());
            }

            case RESET_PASSWORD -> {
                if (!userRepo.existsByEmail(email)) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Email không tồn tại");
                }
                otpService.sendOtp(email, OtpType.RESET_PASSWORD, Optional.empty());
            }

            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại OTP không hợp lệ");
        }
    }

}
