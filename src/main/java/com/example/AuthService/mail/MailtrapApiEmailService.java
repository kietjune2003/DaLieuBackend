package com.example.AuthService.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Profile("prod") // chỉ chạy khi SPRING_PROFILES_ACTIVE=pro
public class MailtrapApiEmailService implements EmailService {

    private final RestTemplate rest = new RestTemplate();

    @Value("${mailtrap.api.url}")
    private String apiUrl; // vd: https://send.api.mailtrap.io/api/send

    @Value("${mailtrap.api.token}")
    private String apiToken; // từ ENV: MAILTRAP_API_TOKEN

    @Value("${mailtrap.from.email}")
    private String fromEmail; // nên là email thuộc domain đã verify

    @Value("${mailtrap.from.name:AuthService}")
    private String fromName;

    @Override
    public void send(String to, String subject, String html) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("from", Map.of("email", fromEmail, "name", fromName));
            body.put("to", List.of(Map.of("email", to)));
            body.put("subject", subject);
            body.put("html", html);
            body.put("category", "transactional");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // Mailtrap API dùng header 'Api-Token', không phải Authorization: Bearer
            headers.add("Api-Token", apiToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = rest.postForEntity(apiUrl, entity, String.class);

            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Mailtrap API error: " + resp.getStatusCode() + " - " + resp.getBody());
            }
        } catch (RestClientResponseException ex) {
            // Bắt body lỗi từ Mailtrap
            throw new RuntimeException("Mailtrap API error: " + ex.getRawStatusCode() + " - " + ex.getResponseBodyAsString(), ex);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email via Mailtrap API", e);
        }
    }
}
