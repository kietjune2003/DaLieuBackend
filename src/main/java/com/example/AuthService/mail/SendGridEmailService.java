package com.example.AuthService.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Profile("prod")
@RequiredArgsConstructor
public class SendGridEmailService implements EmailService {

    @Value("${sendgrid.api.key}")
    private String apiKey;

    @Value("${sendgrid.from.email}")
    private String fromEmail;

    @Value("${sendgrid.from.name:}")
    private String fromName;

    private final RestTemplate rest = new RestTemplate();

    @Override
    public void send(String to, String subject, String html) {
        String url = "https://api.sendgrid.com/v3/mail/send";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> from = new HashMap<>();
        from.put("email", fromEmail);
        if (fromName != null && !fromName.isBlank()) {
            from.put("name", fromName);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("from", from);
        body.put("subject", subject);
        body.put("personalizations",
                List.of(Map.of("to", List.of(Map.of("email", to)))));
        body.put("content", List.of(Map.of(
                "type", "text/html",
                "value", html
        )));

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp;
        try {
            resp = rest.postForEntity(url, req, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call SendGrid API", e);
        }

        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("SendGrid API error: "
                    + resp.getStatusCodeValue() + " - "
                    + (resp.getBody() == null ? "" : resp.getBody()));
        }
    }
}
