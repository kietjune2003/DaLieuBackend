package com.example.AuthService.dto.request;

import com.example.AuthService.dto.response.ScheduleResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleHistoryDTO {
    private LocalDate date;
    private List<ScheduleResponseDTO> schedules;
}
