package com.example.AuthService.service.impl;

import com.example.AuthService.entity.DrugInPrescription;
import com.example.AuthService.entity.Schedule;
import com.example.AuthService.enums.FrequencyType;
import com.example.AuthService.repository.DrugInPrescriptionRepository;
import com.example.AuthService.repository.ScheduleRepository;
import com.example.AuthService.service.ScheduleAutoService;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleAutoServiceImpl implements ScheduleAutoService {

    private final ScheduleRepository scheduleRepository;
    private final DrugInPrescriptionRepository drugInPrescriptionRepository;

    @PostConstruct
    public void test() {
        System.out.println("🟢 ScheduleAutoServiceImpl loaded");
    }

    /**
     * ⏰ Chạy mỗi ngày lúc 00:01
     * 1️⃣ Đánh dấu schedule cũ là SKIPPED
     * 2️⃣ Sinh thêm schedule cho thuốc dài hạn (endDate = NULL)
     */
    @Override
    @Transactional
    @Scheduled(cron = "0 1 0 * * ?")
    public void autoMarkSkippedSchedules() {

        // ===== 1️⃣ AUTO SKIP =====
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        int updated = scheduleRepository.autoMarkSkipped(todayStart);
        System.out.println("✅ Auto skipped schedules: " + updated);

        // ===== 2️⃣ AUTO EXTEND =====
        autoExtendSchedules();
    }

    /**
     * 🔄 Sinh thêm schedule cho các thuốc dài hạn
     */
    private void autoExtendSchedules() {

        List<DrugInPrescription> longTermDrugs =
                drugInPrescriptionRepository.findByEndDateIsNull();

        for (DrugInPrescription dip : longTermDrugs) {

            LocalDateTime lastScheduleDate =
                    scheduleRepository.findLastScheduleDate(dip.getId());

            if (lastScheduleDate == null) continue;

            long daysLeft = ChronoUnit.DAYS.between(
                    LocalDate.now(),
                    lastScheduleDate.toLocalDate()
            );

            // 📌 Nếu lịch còn <= 3 ngày → sinh thêm
            if (daysLeft <= 3) {
                List<Schedule> newSchedules =
                        generateNextSchedules(dip, lastScheduleDate.toLocalDate().plusDays(1));

                if (!newSchedules.isEmpty()) {
                    scheduleRepository.saveAll(newSchedules);
                    System.out.println("➕ Extended schedules for DIP id = " + dip.getId());
                }
            }
        }
    }

    /**
     * 📅 Sinh thêm 7 ngày schedule (window tiếp theo)
     */
    private List<Schedule> generateNextSchedules(
            DrugInPrescription drugInPres,
            LocalDate startDate
    ) {
        LocalDate endDate = startDate.plusDays(7);

        return generateSchedulesCore(
                drugInPres,
                startDate,
                endDate
        );
    }

    /**
     * 🔸 CORE FUNCTION
     * Sinh schedule dựa trên:
     * - khoảng ngày
     * - frequency
     * - giờ uống (mặc định 08:00)
     */
    private List<Schedule> generateSchedulesCore(
            DrugInPrescription drugInPres,
            LocalDate start,
            LocalDate end
    ) {
        List<Schedule> schedules = new ArrayList<>();

        FrequencyType frequencyType =
                (drugInPres.getFrequencyType() != null)
                        ? drugInPres.getFrequencyType()
                        : FrequencyType.DAILY;

        List<LocalDate> targetDates =
                getTargetDates(start, end, frequencyType, drugInPres);

        for (LocalDate date : targetDates) {

            // 👉 Hiện tại dùng mặc định 08:00
            // Nếu sau này có bảng giờ uống → thay đoạn này
            Schedule schedule = Schedule.builder()
                    .drugInPrescription(drugInPres)
                    .date(LocalDateTime.of(date, LocalTime.of(8, 0)))
                    .dosage(1.0)
                    .status(0)      // nên đổi sang enum sau
                    .editted(false)
                    .build();

            schedules.add(schedule);
        }

        return schedules;
    }

    /**
     * 🔹 Lấy danh sách ngày theo frequency
     * (Bạn đã có sẵn – dùng lại)
     */
    private List<LocalDate> getTargetDates(
            LocalDate start,
            LocalDate end,
            FrequencyType frequencyType,
            DrugInPrescription drugInPres
    ) {
        // 👉 GIỮ NGUYÊN logic bạn đang có
        // ví dụ:
        // DAILY → mỗi ngày
        // EVERY_OTHER_DAY → cách ngày
        // WEEKLY → theo thứ
        return new ArrayList<>();
    }
}
