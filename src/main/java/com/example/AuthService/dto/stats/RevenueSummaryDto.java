package com.example.AuthService.dto.stats;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RevenueSummaryDto {
    private BigDecimal grossRevenue;   // doanh thu (sales or cashflow gross)
    private BigDecimal refundAmount;   // hoàn tiền
    private BigDecimal netRevenue;     // gross - refund

    private BigDecimal vnpayRevenue;   // chỉ để hiển thị breakdown
    private BigDecimal codRevenue;

    private long ordersCount;          // số đơn được tính
    private BigDecimal aov;
    private BigDecimal grossCogs;      // tiền vốn của các đơn bán (gross)
    private BigDecimal refundedCogs;   // tiền vốn của các đơn refunded (approx theo createdAt như refund)
    private BigDecimal netCogs;        // grossCogs - refundedCogs

    private BigDecimal grossProfit;    // grossRevenue - grossCogs
    private BigDecimal netProfit;      // netRevenue - netCogs

    private BigDecimal netMarginPct;   // netProfit / netRevenue * 100


}
