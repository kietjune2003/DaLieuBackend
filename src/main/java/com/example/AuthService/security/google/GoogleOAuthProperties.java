package com.example.AuthService.security.google;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@Getter @Setter
@ConfigurationProperties(prefix = "google")
public class GoogleOAuthProperties {
    /**
     * Danh sách client-id hợp lệ (phân tách bằng dấu phẩy trong application.properties)
     */
    private String clientIds;

    public List<String> getClientIdList() {
        if (clientIds == null || clientIds.isBlank()) return List.of();
        return Arrays.stream(clientIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
