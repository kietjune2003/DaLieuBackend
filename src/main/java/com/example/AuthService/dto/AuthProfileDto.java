package com.example.AuthService.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthProfileDto {
    private boolean authenticated;
    private String provider;     // "google" | "local/jwt" | ...
    private String email;
    private String name;
    private String picture;
    private String sub;          // google account id
    private List<String> authorities;
}
