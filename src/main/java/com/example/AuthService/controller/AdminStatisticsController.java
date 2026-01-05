package com.example.AuthService.controller;

import com.example.AuthService.dto.stats.*;
import com.example.AuthService.service.AdminStatisticsService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/statistics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatisticsController {

    private final AdminStatisticsService adminStatisticsService;

    public AdminStatisticsController(AdminStatisticsService adminStatisticsService) {
        this.adminStatisticsService = adminStatisticsService;
    }

    @GetMapping("/revenue/summary")
    public ResponseEntity<?> revenueSummary(@ModelAttribute RevenueStatsFilter filter) {
        return ResponseEntity.ok(adminStatisticsService.getRevenueSummary(filter));
    }

    @GetMapping("/revenue/timeseries")
    public ResponseEntity<?> revenueTimeSeries(@ModelAttribute RevenueStatsFilter filter) {
        return ResponseEntity.ok(adminStatisticsService.getRevenueTimeSeries(filter));
    }

    @GetMapping("/revenue/top-products")
    public ResponseEntity<?> topProducts(@ModelAttribute RevenueStatsFilter filter) {
        return ResponseEntity.ok(adminStatisticsService.getTopProducts(filter));
    }
}

