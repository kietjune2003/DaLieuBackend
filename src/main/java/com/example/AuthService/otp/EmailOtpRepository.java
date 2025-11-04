package com.example.AuthService.otp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

    // Bản ghi OTP còn hạn, chưa dùng
    Optional<EmailOtp> findFirstByEmailAndTypeAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
            String email, OtpType type, LocalDateTime now
    );
}
