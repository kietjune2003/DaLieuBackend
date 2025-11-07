package com.example.AuthService.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrugSummaryResponse {
    private String drugName;
    private LocalDateTime nearestTime;
}
