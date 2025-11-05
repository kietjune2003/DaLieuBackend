package com.example.AuthService.config;

import com.example.AuthService.otp.OtpProperties;

import com.example.AuthService.security.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, OtpProperties.class})
public class AppConfig {

}
