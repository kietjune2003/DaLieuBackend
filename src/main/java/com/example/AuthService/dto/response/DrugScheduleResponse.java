package com.example.AuthService.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrugScheduleResponse {
    private LocalTime time;
    private double dosage;
    private String unitName;
}
