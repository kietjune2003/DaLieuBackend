package com.example.AuthService.otp;

import com.example.AuthService.mail.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final EmailOtpRepository repo;
    private final OtpProperties props;
    private final EmailService emailService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final SecureRandom rnd = new SecureRandom();

    private String genCode() {
        int n = 100000 + rnd.nextInt(900000); // 6 digits
        return String.valueOf(n);
    }

    public void sendOtp(String email, OtpType type, Optional<Map<String, Object>> payloadOpt) {
        var now = LocalDateTime.now();

        // Chống spam: nếu còn một OTP chưa hết hạn trong cooldown thì từ chối
        var last = repo.findFirstByEmailAndTypeAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
                email, type, now
        );
        if (last.isPresent()) {
            var secondsLeft = java.time.Duration.between(now, last.get().getExpiresAt()).toSeconds();
            if (secondsLeft > (props.getRegisterTtlSeconds() - props.getResendCooldownSeconds())) {
                throw new IllegalStateException("Vui lòng thử lại sau (đang trong thời gian chờ gửi lại).");
            }
        }

        String code = genCode();
        int ttl = (type == OtpType.REGISTER) ? props.getRegisterTtlSeconds() : props.getResetTtlSeconds();

        String payloadJson = null;
        if (payloadOpt.isPresent()) {
            try { payloadJson = mapper.writeValueAsString(payloadOpt.get()); }
            catch (Exception e) { throw new RuntimeException("Cannot write payload json", e); }
        }

        var otp = EmailOtp.builder()
                .email(email)
                .type(type)
                .code(code)
                .expiresAt(now.plusSeconds(ttl))
                .payloadJson(payloadJson)
                .build();
        repo.save(otp);

        // Gửi email
        String subject = (type == OtpType.REGISTER) ? "Xác minh đăng ký" : "Mã đặt lại mật khẩu";
        String html = "<p>Xin chào,</p>"
                + "<p>Mã OTP của bạn là: <b>" + code + "</b></p>"
                + "<p>Hết hạn sau " + (ttl/60) + " phút.</p>";
        emailService.send(email, subject, html);
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
