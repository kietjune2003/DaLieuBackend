package com.example.AuthService.service;

public interface ScheduleAutoService {

    /**
     * Tự động đánh dấu các lịch cũ chưa tương tác là bỏ qua
     */
    void autoMarkSkippedSchedules();
}
