package com.example.AuthService.dto.response;

import com.example.AuthService.enums.FrequencyType;
import lombok.Data;

import java.util.List;

@Data
public class ScheduleResponseDTO {
    private Long id;
    private Long scheduleId;
    private String drugName;
    private double dosage;
    private String time;
    private int status;
    private boolean edited;

    private String prescriptionName;
    private String unitName;
    private String note;

    // THÔNG TIN TẦN SUẤT UỐNG THUỐC
    private FrequencyType frequencyType; // DAILY, INTERVAL, WEEKLY
    private Integer intervalDays;         // dùng cho INTERVAL
    private List<String> daysOfWeek;       // dùng cho WEEKLY
}
