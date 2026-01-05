package com.example.AuthService.dto.stats;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TopProductDto {
    private Long drugId;
    private String drugName;
    private long quantity;
    private BigDecimal revenue;
    private BigDecimal cogs;
    private BigDecimal profit;
    private BigDecimal marginPct;
}
