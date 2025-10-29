package com.example.AuthService.mail;

public interface EmailService {
    void send(String to, String subject, String htmlContent);
}
