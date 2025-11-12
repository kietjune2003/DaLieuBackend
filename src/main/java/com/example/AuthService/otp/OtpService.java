package com.example.AuthService.otp;

import com.example.AuthService.mail.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final EmailOtpRepository repo;
    private final OtpProperties props;
    private final EmailService emailService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final SecureRandom rnd = new SecureRandom();

    // Khóa theo (type + email) để tránh gửi đồng thời
    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    private String genCode() {
        int n = 100000 + rnd.nextInt(900000); // Mã 6 chữ số
        return String.valueOf(n);
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public void sendOtp(String email, OtpType type, Optional<Map<String, Object>> payloadOpt) {
        System.out.println("🟢 Calling sendOtp for " + email + ", type=" + type);

        final String key = type + ":" + email;
        final ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            final LocalDateTime now = nowUtc();

            // TTL khác nhau cho từng loại OTP
            final int ttl = (type == OtpType.REGISTER)
                    ? props.getRegisterTtlSeconds()
                    : props.getResetTtlSeconds();

            // ✅ Giới hạn gửi lại mỗi 60 giây (1 phút)
            final int cooldown = 60; // hoặc props.getResendCooldownSeconds()

            var lastOpt = repo.findFirstByEmailAndTypeAndUsedFalseAndExpiresAtAfterOrderByIdDesc(email, type, now);
            if (lastOpt.isPresent()) {
                var last = lastOpt.get();

                // Thời gian phát hành (issue time)
                LocalDateTime issuedAt = (last.getCreatedAt() != null)
                        ? last.getCreatedAt()
                        : last.getExpiresAt().minusSeconds(ttl);


            }

            // Sinh OTP mới
            String code = genCode();
            String payloadJson = payloadOpt.map(p -> {
                try {
                    return mapper.writeValueAsString(p);
                } catch (Exception e) {
                    throw new RuntimeException("Cannot write payload json", e);
                }
            }).orElse(null);

            // Lưu OTP mới
            var otp = EmailOtp.builder()
                    .email(email)
                    .type(type)
                    .code(code)
                    .createdAt(now)
                    .expiresAt(now.plusSeconds(ttl))
                    .payloadJson(payloadJson)
                    .used(false)
                    .attempts(0)
                    .build();
            repo.save(otp);

            // Gửi email
            String subject = (type == OtpType.REGISTER) ? "Xác minh đăng ký" : "Mã đặt lại mật khẩu";
            String html = """
                    <p>Xin chào,</p>
                    <p>Mã OTP của bạn là: <b>%s</b></p>
                    <p>Hết hạn sau %d phút.</p>
                    """.formatted(code, ttl / 60);

            emailService.send(email, subject, html);
            System.out.println("✉️ Sending email OTP to " + email + " with code=" + code);

        } finally {
            lock.unlock();
            if (!lock.hasQueuedThreads()) {
                locks.remove(key, lock);
            }
        }
    }

    public EmailOtp verify(String email, OtpType type, String code) {
        final LocalDateTime now = nowUtc();

        var otp = repo.findFirstByEmailAndTypeAndUsedFalseAndExpiresAtAfterOrderByIdDesc(email, type, now)
                .orElseThrow(() -> new IllegalStateException("OTP không tồn tại hoặc đã hết hạn."));

        if (!otp.getCode().equals(code)) {
            otp.setAttempts(otp.getAttempts() + 1);
            repo.save(otp);
            if (otp.getAttempts() >= props.getMaxAttempts()) {
                otp.setUsed(true); // khoá OTP sau quá số lần thử
                repo.save(otp);
            }
            throw new IllegalArgumentException("OTP không đúng.");
        }

        otp.setUsed(true);
        repo.save(otp);
        return otp;
    }
}
