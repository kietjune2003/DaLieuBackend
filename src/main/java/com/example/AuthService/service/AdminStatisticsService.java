package com.example.AuthService.service;

import com.example.AuthService.dto.stats.*;
import org.springframework.data.domain.Page;

public interface AdminStatisticsService {

    RevenueSummaryDto getRevenueSummary(RevenueStatsFilter filter);

    RevenueTimeSeriesDto getRevenueTimeSeries(RevenueStatsFilter filter);

    Page<TopProductDto> getTopProducts(RevenueStatsFilter filter);


}
