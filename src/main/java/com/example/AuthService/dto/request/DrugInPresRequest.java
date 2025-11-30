package com.example.AuthService.dto.request;

import com.example.AuthService.enums.FrequencyType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrugInPresRequest {
    @JsonProperty("drug_name")
    private String drugName;

    @JsonProperty("unit_id")
    private Long unitId;

    @JsonProperty("start_date")
    private String startDate;

    @JsonProperty("end_date")
    private String endDate;

    private String note;

    // Danh sách lịch uống (nếu nhập cụ thể theo giờ/ngày)
    private List<ScheduleAddRequest> schedules;

    // 🔹 Thêm nhóm thuộc tính cho "tần suất"
    @JsonProperty("frequency_type")
    private FrequencyType frequencyType; // DAILY, INTERVAL, WEEKLY

    // Nếu frequencyType = INTERVAL → dùng số ngày cách nhau (ví dụ 2 nghĩa là cách 2 ngày uống 1 lần)
    @JsonProperty("interval_days")
    private Integer intervalDays;

    // Nếu frequencyType = WEEKLY → danh sách các thứ trong tuần (ví dụ: ["MONDAY", "WEDNESDAY", "FRIDAY"])
    @JsonProperty("days_of_week")
    private List<String> daysOfWeek;


}
