package com.example.AuthService.dto.request;

import lombok.Data;

import java.util.Date;

@Data
public class ScheduleAddRequest {
    private String time;
    private Double dosage;
}