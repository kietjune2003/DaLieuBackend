package com.example.AuthService.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UserResponseDTO {
    private Long id;
    private String email;
    private String password;
    private String name;
    private String gender;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String photoUrl;
    private String roleName;
    private String countryName;
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private boolean enabled;
}
