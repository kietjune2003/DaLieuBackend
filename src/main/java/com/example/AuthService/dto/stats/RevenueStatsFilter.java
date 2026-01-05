package com.example.AuthService.dto.stats;

import com.example.AuthService.enums.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class RevenueStatsFilter {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime to;

    private StatsGroupBy groupBy = StatsGroupBy.DAY;

    private StatsMode mode = StatsMode.SALES_ALL;

    private Set<PaymentMethod> paymentMethods; // COD/VNPAY
    private Set<PaymentStatus> paymentStatuses; // lọc payments table (optional)
    private Set<OrderStatus> orderStatuses; // lọc orders table (optional)

    private Set<Long> drugIds;
    private Long userId;

    private Integer topN = 10;
}
