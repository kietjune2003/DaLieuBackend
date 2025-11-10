package com.example.AuthService.service.impl;

import com.example.AuthService.dto.request.DrugInPresRequest;
import com.example.AuthService.dto.request.PrescriptionRequest;
import com.example.AuthService.dto.request.ScheduleAddRequest;
import com.example.AuthService.dto.response.DrugSummaryResponse;
import com.example.AuthService.dto.response.PrescriptionSummaryResponse;
import com.example.AuthService.entity.*;
import com.example.AuthService.enums.FrequencyType;
import com.example.AuthService.repository.*;
import com.example.AuthService.service.PrescriptionService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final DrugInPrescriptionRepository drugInPrescriptionRepository;
    private final ScheduleRepository scheduleRepository;
    private final DrugRepository drugRepository;
    private final UnitRepository unitRepository;

    @Override
    @Transactional
    public Prescription createPrescription(PrescriptionRequest request, User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null when creating prescription");
        }
        if (request.getDrugs() == null || request.getDrugs().isEmpty()) {
            throw new IllegalArgumentException("Prescription must contain at least one drug");
        }

        // 🔹 Tạo đơn thuốc mới
        Prescription prescription = new Prescription();
        prescription.setName(request.getName());
        prescription.setHospital(request.getHospital());
        prescription.setDoctorName(request.getDoctorName());
        prescription.setConsultationDate(parseLocalDate(request.getConsultationDate()));
        prescription.setFollowUpDate(parseLocalDate(request.getFollowUpDate()));
        prescription.setUser(user);

        Prescription savedPrescription = prescriptionRepository.save(prescription);

        // 🔹 Duyệt qua danh sách thuốc
        for (DrugInPresRequest drugReq : request.getDrugs()) {

            if (drugReq.getDrugId() == null) {
                throw new IllegalArgumentException(" Drug ID must not be null");
            }
            if (drugReq.getUnitId() == null) {
                throw new IllegalArgumentException(" Unit ID must not be null");
            }

            Drug drug = drugRepository.findById(drugReq.getDrugId())
                    .orElseThrow(() -> new IllegalArgumentException(" Drug not found with ID: " + drugReq.getDrugId()));

            Unit unit = unitRepository.findById(drugReq.getUnitId())
                    .orElseThrow(() -> new IllegalArgumentException(" Unit not found with ID: " + drugReq.getUnitId()));

            LocalDate startDate = parseLocalDate(drugReq.getStartDate());
            LocalDate endDate = (drugReq.getEndDate() != null && !drugReq.getEndDate().isEmpty())
                    ? parseLocalDate(drugReq.getEndDate())
                    : startDate.plusDays(7);

            DrugInPrescription drugInPrescription = new DrugInPrescription();
            drugInPrescription.setPrescription(savedPrescription);
            drugInPrescription.setDrug(drug);
            drugInPrescription.setUnit(unit);
            drugInPrescription.setStartDate(startDate);
            drugInPrescription.setEndDate(endDate);
            drugInPrescription.setNote(drugReq.getNote());
            drugInPrescription.setFrequencyType(
                    drugReq.getFrequencyType() != null ? drugReq.getFrequencyType() : FrequencyType.DAILY
            );
            drugInPrescription.setIntervalDays(drugReq.getIntervalDays());
            drugInPrescription.setDaysOfWeek(
                    drugReq.getDaysOfWeek() != null ? drugReq.getDaysOfWeek() : new ArrayList<>()
            );

            DrugInPrescription savedDrugInPres = drugInPrescriptionRepository.save(drugInPrescription);

            // 🔹 Sinh lịch uống thuốc
            List<Schedule> generatedSchedules = generateSchedules(drugReq, savedDrugInPres);
            scheduleRepository.saveAll(generatedSchedules);
        }

        return savedPrescription;
    }


    /**
     * 🔸 Sinh danh sách Schedule từ DrugInPresRequest (theo frequency + time uống)
     */
    private List<Schedule> generateSchedules(DrugInPresRequest drugReq, DrugInPrescription drugInPres) {
        List<Schedule> schedules = new ArrayList<>();

        LocalDate start = parseLocalDate(drugReq.getStartDate());
        LocalDate end = (drugReq.getEndDate() != null && !drugReq.getEndDate().isEmpty())
                ? parseLocalDate(drugReq.getEndDate())
                : start.plusDays(7);

        FrequencyType frequencyType =
                (drugReq.getFrequencyType() != null) ? drugReq.getFrequencyType() : FrequencyType.DAILY;

        // 🔹 Lấy danh sách ngày cần tạo schedule
        List<LocalDate> targetDates = getTargetDates(start, end, frequencyType, drugReq);

        for (LocalDate date : targetDates) {
            if (drugReq.getSchedules() != null && !drugReq.getSchedules().isEmpty()) {
                for (ScheduleAddRequest timeReq : drugReq.getSchedules()) {
                    try {
                        LocalTime time = LocalTime.parse(timeReq.getTime());
                        LocalDateTime dateTime = LocalDateTime.of(date, time);

                        Schedule schedule = Schedule.builder()
                                .drugInPrescription(drugInPres)
                                .date(dateTime)
                                .dosage(timeReq.getDosage() != null ? timeReq.getDosage() : 1.0)
                                .status(0)
                                .editted(false)
                                .build();

                        schedules.add(schedule);
                    } catch (Exception e) {
                        System.err.println("⚠️ Lỗi khi parse giờ uống: " + timeReq.getTime());
                    }
                }
            } else {
                // Nếu không chỉ định giờ uống → mặc định 08:00
                LocalDateTime defaultTime = LocalDateTime.of(date, LocalTime.of(8, 0));
                Schedule schedule = Schedule.builder()
                        .drugInPrescription(drugInPres)
                        .date(defaultTime)
                        .dosage(1.0)
                        .status(0)
                        .editted(false)
                        .build();
                schedules.add(schedule);
            }
        }

        return schedules;
    }

    /**
     * 🔸 Sinh danh sách các ngày uống thuốc dựa theo FrequencyType
     */
    private List<LocalDate> getTargetDates(LocalDate start, LocalDate end,
                                           FrequencyType type, DrugInPresRequest drugReq) {
        List<LocalDate> dates = new ArrayList<>();

        switch (type) {
            case DAILY -> {
                for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                    dates.add(date);
                }
            }
            case INTERVAL -> {
                int interval = (drugReq.getIntervalDays() != null && drugReq.getIntervalDays() > 0)
                        ? drugReq.getIntervalDays() : 2;
                for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(interval)) {
                    dates.add(date);
                }
            }
            case WEEKLY -> {
                List<DayOfWeek> targetDays = new ArrayList<>();
                if (drugReq.getDaysOfWeek() != null && !drugReq.getDaysOfWeek().isEmpty()) {
                    for (String dayStr : drugReq.getDaysOfWeek()) {
                        try {
                            targetDays.add(DayOfWeek.valueOf(dayStr.toUpperCase()));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                if (targetDays.isEmpty()) {
                    targetDays.addAll(Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY));
                }
                for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                    if (targetDays.contains(date.getDayOfWeek())) {
                        dates.add(date);
                    }
                }
            }
        }

        return dates;
    }

    /**
     * 🔸 Hàm parse ngày an toàn (tránh lỗi null)
     */
    private LocalDate parseLocalDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return LocalDate.now();
        return LocalDate.parse(dateStr);
    }

    @Override
    @Transactional
    public void deletePrescription(Long id, User user) {
        Prescription prescription = prescriptionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn thuốc hoặc không có quyền xoá."));

        // JPA sẽ tự xoá các bản ghi con vì cascade = ALL + orphanRemoval = true
        prescriptionRepository.delete(prescription);
    }

    @Override
    public List<PrescriptionSummaryResponse> getPrescriptionsByStatus(User user, Integer status) {
        List<Prescription> prescriptions = prescriptionRepository.findByUserAndStatus(user, status);
        List<PrescriptionSummaryResponse> result = new ArrayList<>();
        // 🔹 Sắp xếp theo ngày tạo giảm dần (mới nhất trước)
        prescriptions.sort(Comparator.comparing(Prescription::getCreatedAt).reversed());
        for (Prescription prescription : prescriptions) {
            List<DrugSummaryResponse> drugSummaries = new ArrayList<>();

            for (DrugInPrescription dip : prescription.getDrugInPrescriptions()) {
                if (dip.getDrug() == null) continue;

                String drugName = dip.getDrug().getName();

                // Tìm giờ uống gần nhất (sắp tới so với hiện tại)
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime nearest = dip.getSchedules().stream()
                        .map(Schedule::getDate)
                        .filter(date -> date.isAfter(now))
                        .sorted()
                        .findFirst()
                        .orElse(null);

                drugSummaries.add(new DrugSummaryResponse(drugName, nearest));
            }

            PrescriptionSummaryResponse summary = new PrescriptionSummaryResponse(
                    prescription.getId(),
                    prescription.getName(),
                    prescription.getDrugInPrescriptions().size(),
                    drugSummaries
            );

            result.add(summary);
        }

        return result;
    }



    @Override
    @Transactional
    public PrescriptionRequest updatePrescription(Long id, PrescriptionRequest request, User user) {
        // 🔹 1. Tìm đơn thuốc theo ID và kiểm tra quyền
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn thuốc với ID: " + id));

        if (!prescription.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền cập nhật đơn thuốc này");
        }

        // 🔹 2. Cập nhật thông tin cơ bản của đơn thuốc
        prescription.setName(request.getName());
        prescription.setHospital(request.getHospital());
        prescription.setDoctorName(request.getDoctorName());
        prescription.setConsultationDate(parseLocalDate(request.getConsultationDate()));
        prescription.setFollowUpDate(parseLocalDate(request.getFollowUpDate()));
        prescription.setUpdatedAt(LocalDateTime.now());

        // 🔹 3. Xóa dữ liệu cũ (bao gồm cả schedules)
        for (DrugInPrescription oldDrug : prescription.getDrugInPrescriptions()) {
            scheduleRepository.deleteAll(oldDrug.getSchedules());
        }
        drugInPrescriptionRepository.deleteAll(prescription.getDrugInPrescriptions());
        prescription.getDrugInPrescriptions().clear();

        // 🔹 4. Thêm lại danh sách thuốc mới
        for (DrugInPresRequest drugReq : request.getDrugs()) {

            Drug drug = drugRepository.findById(drugReq.getDrugId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc với ID: " + drugReq.getDrugId()));
            Unit unit = unitRepository.findById(drugReq.getUnitId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn vị với ID: " + drugReq.getUnitId()));

            LocalDate startDate = parseLocalDate(drugReq.getStartDate());
            LocalDate endDate = (drugReq.getEndDate() != null && !drugReq.getEndDate().isEmpty())
                    ? parseLocalDate(drugReq.getEndDate())
                    : startDate.plusDays(7);

            DrugInPrescription newDrug = new DrugInPrescription();
            newDrug.setPrescription(prescription);
            newDrug.setDrug(drug);
            newDrug.setUnit(unit);
            newDrug.setStartDate(startDate);
            newDrug.setEndDate(endDate);
            newDrug.setNote(drugReq.getNote());
            newDrug.setFrequencyType(
                    drugReq.getFrequencyType() != null ? drugReq.getFrequencyType() : FrequencyType.DAILY
            );
            newDrug.setIntervalDays(drugReq.getIntervalDays());
            newDrug.setDaysOfWeek(
                    drugReq.getDaysOfWeek() != null ? drugReq.getDaysOfWeek() : new ArrayList<>()
            );

            DrugInPrescription savedDrug = drugInPrescriptionRepository.save(newDrug);

            // 🔹 Sinh lại lịch uống dựa theo logic giống createPrescription()
            List<Schedule> newSchedules = generateSchedules(drugReq, savedDrug);
            scheduleRepository.saveAll(newSchedules);

            prescription.getDrugInPrescriptions().add(savedDrug);
        }

        // 🔹 5. Lưu lại đơn thuốc
        prescriptionRepository.save(prescription);

        return request; // Hoặc map sang DTO trả về nếu cần
    }



    @Transactional(readOnly = true)
    public PrescriptionRequest getPrescriptionAsRequestById(Long prescriptionId, User user) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new RuntimeException("Prescription not found with ID: " + prescriptionId));

        // kiểm tra owner
        if (!prescription.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied for this prescription");
        }

        // formatter
        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE; // "yyyy-MM-dd"
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        // build PrescriptionRequest
        PrescriptionRequest resp = PrescriptionRequest.builder()
                .name(prescription.getName())
                .hospital(prescription.getHospital())
                .doctorName(prescription.getDoctorName())
                .consultationDate(prescription.getConsultationDate() != null ? prescription.getConsultationDate().format(dateFormatter) : null)
                .followUpDate(prescription.getFollowUpDate() != null ? prescription.getFollowUpDate().format(dateFormatter) : null)
                .drugs(new ArrayList<>())
                .build();

        // Với mỗi DrugInPrescription -> tạo DrugInPresRequest
        for (DrugInPrescription dip : prescription.getDrugInPrescriptions()) {
            DrugInPresRequest dReq = DrugInPresRequest.builder()
                    .drugId(dip.getDrug() != null ? dip.getDrug().getId() : null)
                    .unitId(dip.getUnit() != null ? dip.getUnit().getId() : null)
                    .startDate(dip.getStartDate() != null ? dip.getStartDate().format(dateFormatter) : null)
                    .endDate(dip.getEndDate() != null ? dip.getEndDate().format(dateFormatter) : null)
                    .note(dip.getNote())
                    .frequencyType(dip.getFrequencyType())
                    .intervalDays(dip.getIntervalDays())
                    .daysOfWeek(dip.getDaysOfWeek() != null ? new ArrayList<>(dip.getDaysOfWeek()) : new ArrayList<>())
                    .schedules(new ArrayList<>())
                    .build();

            // --- đây là phần quan trọng: gộp schedules theo "giờ trong ngày" ---
            // Lấy tất cả schedule entity liên quan, map theo LocalTime -> dosage (giữ dosage đầu tiên gặp)
            Map<LocalTime, Double> timeToDosage = new HashMap<>();
            if (dip.getSchedules() != null) {
                for (Schedule s : dip.getSchedules()) {
                    if (s == null || s.getDate() == null) continue;
                    LocalTime lt = s.getDate().toLocalTime();
                    // nếu đã có key, giữ giá trị hiện có (hoặc bạn có thể replace bằng trung bình / max tuỳ ý)
                    timeToDosage.putIfAbsent(lt, s.getDosage());
                }
            }

            // chuyển map sang list ScheduleAddRequest, sắp xếp theo giờ tăng dần
            List<ScheduleAddRequest> scheduleList = timeToDosage.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> {
                        ScheduleAddRequest sa = new ScheduleAddRequest();
                        sa.setTime(e.getKey().format(timeFormatter)); // "HH:mm"
                        sa.setDosage(e.getValue() != null ? e.getValue() : 1.0);
                        return sa;
                    })
                    .toList();

            dReq.setSchedules(new ArrayList<>(scheduleList));

            resp.getDrugs().add(dReq);
        }

        return resp;
    }


    @Override
    @Transactional
    public Prescription togglePrescriptionStatus(Long id, User user) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn thuốc với ID: " + id));

        // Kiểm tra quyền sở hữu
        if (!prescription.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền thay đổi trạng thái đơn thuốc này");
        }

        // Đảo trạng thái: nếu 1 -> 0, nếu 0 -> 1
        Integer newStatus = (prescription.getStatus() != null && prescription.getStatus() == 1) ? 0 : 1;
        prescription.setStatus(newStatus);

        prescriptionRepository.save(prescription);
        return prescription;
    }


}
