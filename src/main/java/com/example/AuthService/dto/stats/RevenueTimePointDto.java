package com.example.AuthService.dto.stats;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class RevenueTimePointDto {
    private LocalDate bucket;          // ngày bắt đầu bucket (day/week/month)
    private BigDecimal vnpayGross;
    private BigDecimal codRevenue;
    private BigDecimal refundAmount;
    private BigDecimal netRevenue;
    private BigDecimal grossCogs;
    private BigDecimal refundedCogs;
    private BigDecimal netCogs;

    private BigDecimal netProfit;
    private BigDecimal netMarginPct;

}
