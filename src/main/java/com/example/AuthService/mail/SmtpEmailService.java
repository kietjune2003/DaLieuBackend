package com.example.AuthService.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@Profile("!prod")
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@localhost.local}")
    private String fromAddress;

    @Override
    public void send(String to, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, "UTF-8");
            h.setFrom(fromAddress);
            h.setTo(to);
            h.setSubject(subject);
            h.setText(html, true); // HTML
            mailSender.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }
}

