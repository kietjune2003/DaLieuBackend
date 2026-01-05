package com.example.AuthService.dto.stats;

import com.example.AuthService.enums.StatsGroupBy;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RevenueTimeSeriesDto {
    private StatsGroupBy groupBy;
    private List<RevenueTimePointDto> points;
}
