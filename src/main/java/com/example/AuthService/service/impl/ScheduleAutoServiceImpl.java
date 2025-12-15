package com.example.AuthService.service.impl;

import com.example.AuthService.repository.ScheduleRepository;
import com.example.AuthService.service.ScheduleAutoService;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ScheduleAutoServiceImpl implements ScheduleAutoService {

    private final ScheduleRepository scheduleRepository;
    @PostConstruct
    public void test() {
        System.out.println("🟢 ScheduleAutoServiceImpl loaded");
    }
    /**
     * Chạy mỗi ngày lúc 00:01
     */
    @Override
    @Transactional
    @Scheduled(cron = "0 1 0 * * ?")
    public void autoMarkSkippedSchedules() {

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        int updated = scheduleRepository.autoMarkSkipped(todayStart);

        System.out.println("✅ Auto skipped schedules: " + updated);
    }
}
