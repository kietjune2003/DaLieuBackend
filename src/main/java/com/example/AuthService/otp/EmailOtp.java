package com.example.AuthService.otp;

import com.example.AuthService.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_otps",
        indexes = {
                @Index(name="idx_email_otps_email_type", columnList = "email,type"),
                @Index(name="idx_email_otps_expires_at", columnList = "expiresAt")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailOtp  {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=32)
    private OtpType type;           // REGISTER | RESET_PASSWORD

    @Column(nullable=false, length=10)
    private String code;

    // NEW: thời điểm phát hành OTP
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime createdAt;

    @Column(nullable=false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(nullable=false)
    private int attempts = 0;

    @Builder.Default
    @Column(nullable=false)
    private boolean used = false;

    @Lob
    private String payloadJson; // optional
}
