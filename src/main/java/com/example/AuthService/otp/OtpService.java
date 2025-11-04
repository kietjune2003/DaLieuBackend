package com.example.AuthService.otp;

import com.example.AuthService.mail.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
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

    // Khoá theo (type + email) để chặn xử lý song song trong cùng JVM
    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    private String genCode() {
        int n = 100000 + rnd.nextInt(900000); // 6 digits
        return String.valueOf(n);
    }

    public void sendOtp(String email, OtpType type, Optional<Map<String, Object>> payloadOpt) {
        final String key = type + ":" + email;
        final ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            final LocalDateTime now = LocalDateTime.now();

            // TTL theo loại OTP
            final int ttl = (type == OtpType.REGISTER)
                    ? props.getRegisterTtlSeconds()
                    : props.getResetTtlSeconds();

            final int cooldown = props.getResendCooldownSeconds();

            // Tìm OTP chưa dùng và còn hạn
            var lastOpt = repo.findFirstByEmailAndTypeAndUsedFalseAndExpiresAtAfterOrderByIdDesc(email, type, now);
            if (lastOpt.isPresent()) {
                var last = lastOpt.get();

                // Tính thời điểm phát hành gần nhất: issuedAt = expiresAt - TTL
                var issuedAt = last.getExpiresAt().minusSeconds(ttl);
                long elapsed = Duration.between(issuedAt, now).getSeconds();

                // 1) Idempotent window: nếu 2 request đến trong 3s, coi như 1 lần -> không gửi lại, không ném lỗi
                if (elapsed >= 0 && elapsed < 3) {
                    return;
                }

                // 2) Cooldown: nếu chưa qua thời gian chờ gửi lại -> ném lỗi
                long remaining = cooldown - elapsed;
                if (remaining > 0) {
                    throw new IllegalStateException(
                            "Vui lòng thử lại sau " + remaining + " giây (đang trong thời gian chờ gửi lại)."
                    );
                }
            }

            // Sinh mã & payload
            String code = genCode();

            String payloadJson = null;
            if (payloadOpt.isPresent()) {
                try {
                    payloadJson = mapper.writeValueAsString(payloadOpt.get());
                } catch (Exception e) {
                    throw new RuntimeException("Cannot write payload json", e);
                }
            }

            // Lưu OTP (chưa dùng) với expiresAt = now + TTL
            var otp = EmailOtp.builder()
                    .email(email)
                    .type(type)
                    .code(code)
                    .expiresAt(now.plusSeconds(ttl))
                    .payloadJson(payloadJson)
                    .used(false)
                    .attempts(0)
                    .build();
            repo.save(otp);

            // Gửi email (ở 'pro' sẽ dùng Mailtrap API; ở dev/local dùng SMTP)
            String subject = (type == OtpType.REGISTER) ? "Xác minh đăng ký" : "Mã đặt lại mật khẩu";
            String html = "<p>Xin chào,</p>"
                    + "<p>Mã OTP của bạn là: <b>" + code + "</b></p>"
                    + "<p>Hết hạn sau " + (ttl / 60) + " phút.</p>";
            emailService.send(email, subject, html);

        } finally {
            lock.unlock();
            if (!lock.hasQueuedThreads()) {
                locks.remove(key, lock);
            }
        }
    }

    public EmailOtp verify(String email, OtpType type, String code) {
        var now = LocalDateTime.now();
        var otp = repo.findFirstByEmailAndTypeAndUsedFalseAndExpiresAtAfterOrderByIdDesc(email, type, now)
                .orElseThrow(() -> new IllegalStateException("OTP không tồn tại hoặc đã hết hạn."));

        if (!otp.getCode().equals(code)) {
            otp.setAttempts(otp.getAttempts() + 1);
            repo.save(otp);
            if (otp.getAttempts() >= props.getMaxAttempts()) {
                otp.setUsed(true); // khoá OTP
                repo.save(otp);
            }
            throw new IllegalArgumentException("OTP không đúng.");
        }

        // OK
        otp.setUsed(true);
        repo.save(otp);
        return otp;
    }
}
