package com.example.AuthService.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleResponseDTO {

    private Long scheduleId;
    private String drugName;
    private double dosage;
    private String time;
    private int status;
    private boolean edited;
    private String prescriptionName;
}
