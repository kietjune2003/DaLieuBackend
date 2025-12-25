package com.example.AuthService.dto.request;

import lombok.Data;

@Data
public class UserUpdateRequestDTO {
    private String name;
    private String email;
    private String gender;
    private String password;
    private String phoneNumber;
    private String photoUrl;
    private Long roleId;
    private Long countryId;
    private Boolean accountNonLocked;
    private Boolean enabled;
}
