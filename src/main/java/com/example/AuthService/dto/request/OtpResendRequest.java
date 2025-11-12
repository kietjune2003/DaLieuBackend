package com.example.AuthService.dto.request;


import com.example.AuthService.otp.OtpType;
import lombok.Data;

@Data
public class OtpResendRequest {
    private String email;
    private OtpType type; // REGISTER hoặc RESET_PASSWORD
}
