package com.example.AuthService.dto.response;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserProfileResponse {
    private String gender;
    private LocalDate dateOfBirth;
}
