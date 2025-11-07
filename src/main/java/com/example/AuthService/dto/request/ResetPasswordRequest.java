package com.example.AuthService.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @Email @NotBlank private String email;
    @NotBlank private String code;
    @NotBlank @Size(min=8) private String newPassword;
}
