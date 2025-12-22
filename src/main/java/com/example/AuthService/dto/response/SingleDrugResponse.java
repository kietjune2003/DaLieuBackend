package com.example.AuthService.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SingleDrugResponse {
    private Long id;
    private String drugName;
    private LocalDateTime nearestTime;
}
