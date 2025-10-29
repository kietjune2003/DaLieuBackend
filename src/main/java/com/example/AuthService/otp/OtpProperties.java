package com.example.AuthService.otp;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "otp")
public class OtpProperties {
    private int registerTtlSeconds = 300;
    private int resetTtlSeconds = 300;
    private int resendCooldownSeconds = 60;
    private int maxAttempts = 5;
}
