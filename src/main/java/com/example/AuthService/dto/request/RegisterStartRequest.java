package com.example.AuthService.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterStartRequest {
    @Email @NotBlank private String email;
    @NotBlank @Size(min=8) private String password;
    @NotBlank private String name;

    private String gender;
    private String phoneNumber;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;
    private Long countryId;
    private String photoUrl;
}
