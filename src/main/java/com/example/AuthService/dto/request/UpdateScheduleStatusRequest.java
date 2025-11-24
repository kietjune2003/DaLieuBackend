package com.example.AuthService.dto.request;

import lombok.Data;

@Data
public class UpdateScheduleStatusRequest {
    private Long scheduleId;
    private Integer status; // 0 or 1
}
