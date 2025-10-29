package com.example.AuthService.otp;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_otps",
        indexes = {
                @Index(name="idx_email_otps_email_type", columnList = "email,type"),
                @Index(name="idx_email_otps_expires_at", columnList = "expiresAt")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailOtp {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=32)
    private OtpType type;           // REGISTER | RESET_PASSWORD

    @Column(nullable=false, length=10)
    private String code;            // ví dụ 6 chữ số

    @Column(nullable=false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(nullable=false)
    private int attempts = 0;       // số lần nhập

    @Builder.Default
    @Column(nullable=false)
    private boolean used = false;

    // Dùng để lưu metadata tạm (VD: hash password lúc đăng ký)
    @Lob
    private String payloadJson;     // optional (JSON)
}
