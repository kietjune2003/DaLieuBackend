package com.example.AuthService.dto.response;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenResponse {
    private String tokenType;       // "Bearer"
    private String accessToken;
    private String refreshToken;
    private long   expiresIn;       // ms
    private String provider;        // "google"
    private String email;
    private String name;
    private String picture;
    private String sub;             // google account id
    private List<String> authorities;
}
