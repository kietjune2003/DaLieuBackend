package com.example.AuthService.dto.response;

import com.example.AuthService.enums.FrequencyType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SingleDrugResponse {
    private Long id;
    private String drugName;
    private String note;

    private FrequencyType frequencyType;
    private Integer intervalDays;       // dùng cho INTERVAL
    private List<String> daysOfWeek;     // dùng cho WEEKLY

    private List<DrugScheduleResponse> schedules;
}
