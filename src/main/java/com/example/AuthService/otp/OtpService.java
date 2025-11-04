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

    // Khoá theo (type + email) để chống 2 request đồng thời trong cùng JVM
    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    private String genCode() {
        int n = 100000 + rnd.nextInt(900000); // 6 digits
        return String.valueOf(n);
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public void sendOtp(String email, OtpType type, Optional<Map<String, Object>> payloadOpt) {
        final String key = type + ":" + email;
        final ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            final LocalDateTime now = nowUtc();

            final int ttl = (type == OtpType.REGISTER) ? props.getRegisterTtlSeconds()
                    : props.getResetTtlSeconds();
            final int cooldown = props.getResendCooldownSeconds();

            var lastOpt = repo.findFirstByEmailAndTypeAndUsedFalseAndExpiresAtAfterOrderByIdDesc(email, type, now);
            if (lastOpt.isPresent()) {
                var last = lastOpt.get();

                // Ưu tiên createdAt; fallback sang (expiresAt - TTL) nếu bản ghi cũ chưa có createdAt
                LocalDateTime issuedAt = (last.getCreatedAt() != null)
                        ? last.getCreatedAt()
                        : last.getExpiresAt().minusSeconds(ttl);

                long elapsed = Math.max(0, Duration.between(issuedAt, now).getSeconds());

                // Idempotent window 3s: nếu 2 request sát nhau, coi như 1 lần gửi (không ném lỗi, không gửi lại)
                if (elapsed < 3) {
                    return;
                }

                long remaining = cooldown - elapsed;
                if (remaining > 0) {
                    throw new IllegalStateException(
                            "Vui lòng thử lại sau " + remaining + " giây (đang trong thời gian chờ gửi lại)."
                    );
                }
            }

            // Sinh OTP & payload
            String code = genCode();
            String payloadJson = payloadOpt.map(p -> {
                try {
                    return mapper.writeValueAsString(p);
                } catch (Exception e) {
                    throw new RuntimeException("Cannot write payload json", e);
                }
            }).orElse(null);

            // Lưu OTP mới (expiresAt theo UTC)
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

            // Gửi mail qua implementation của EmailService (SMTP ở dev, Mailtrap API ở prod)
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
