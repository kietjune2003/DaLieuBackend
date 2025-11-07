package com.example.AuthService.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {
    @Email @NotBlank
    private String email;

    @NotBlank
    @Size(min = 8, message = "Password tối thiểu 8 ký tự")
    private String password;

    @NotBlank
    private String name;

    // Tuỳ chọn
    private String gender;
    private String phoneNumber;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private Long countryId;   // nếu người dùng chọn quốc gia
    private String photoUrl;  // nếu có avatar sẵn
}
