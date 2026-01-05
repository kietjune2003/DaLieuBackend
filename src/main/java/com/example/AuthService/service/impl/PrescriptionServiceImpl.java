package com.example.AuthService.service.impl;

import com.example.AuthService.dto.request.*;
import com.example.AuthService.dto.response.*;
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
import java.util.stream.Collectors;

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

            // drugName giờ là optional → KHÔNG ép buộc
            // nhưng unitId vẫn nên bắt buộc
            if (drugReq.getUnitId() == null) {
                throw new IllegalArgumentException("Unit ID must not be null");
            }

            Unit unit = unitRepository.findById(drugReq.getUnitId())
                    .orElseThrow(() ->
                            new IllegalArgumentException("Unit not found with ID: " + drugReq.getUnitId())
                    );

            if (drugReq.getStartDate() == null || drugReq.getStartDate().isBlank()) {
                throw new IllegalArgumentException("Start date must not be null");
            }

            LocalDate startDate = parseLocalDate(drugReq.getStartDate());
            LocalDate endDate = (drugReq.getEndDate() != null && !drugReq.getEndDate().isBlank())
                    ? parseLocalDate(drugReq.getEndDate())
                    : startDate.plusDays(7);   // default 7 ngày nếu không truyền

            // 🔹 Map DTO → Entity DrugInPrescription
            DrugInPrescription drugInPrescription = new DrugInPrescription();
            drugInPrescription.setPrescription(savedPrescription);
            drugInPrescription.setUser(user);
            drugInPrescription.setDrugName(drugReq.getDrugName());   // <-- dùng String thay vì entity Drug
            drugInPrescription.setUnit(unit);
            drugInPrescription.setStartDate(startDate);
            drugInPrescription.setEndDate(endDate);
            drugInPrescription.setNote(drugReq.getNote());

            // 🔹 Tần suất (mặc định DAILY nếu không truyền)
            FrequencyType frequencyType =
                    (drugReq.getFrequencyType() != null) ? drugReq.getFrequencyType() : FrequencyType.DAILY;
            drugInPrescription.setFrequencyType(frequencyType);

            // Nếu là INTERVAL thì set intervalDays, còn lại có thể để null cho sạch dữ liệu
            if (frequencyType == FrequencyType.INTERVAL) {
                drugInPrescription.setIntervalDays(drugReq.getIntervalDays());
            } else {
                drugInPrescription.setIntervalDays(null);
            }

            // Nếu là WEEKLY thì lưu daysOfWeek, ngược lại thì để list rỗng
            if (frequencyType == FrequencyType.WEEKLY &&
                    drugReq.getDaysOfWeek() != null &&
                    !drugReq.getDaysOfWeek().isEmpty()) {
                drugInPrescription.setDaysOfWeek(new ArrayList<>(drugReq.getDaysOfWeek()));
            } else {
                drugInPrescription.setDaysOfWeek(new ArrayList<>());
            }

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

        LocalDate end;
        if (drugReq.getEndDate() != null && !drugReq.getEndDate().isEmpty()) {
            end = parseLocalDate(drugReq.getEndDate());
            drugInPres.setEndDate(end); // thuốc theo đợt
        } else {
            end = start.plusDays(7);    // window đầu tiên
            drugInPres.setEndDate(null); // thuốc dài hạn
        }


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
        prescriptions.sort(
                Comparator.comparing(Prescription::getCreatedAt)
                        .reversed()
        );

        // 🔹 Thời điểm hiện tại dùng chung
        LocalDateTime now = LocalDateTime.now();

        for (Prescription prescription : prescriptions) {
            List<DrugSummaryResponse> drugSummaries = new ArrayList<>();

            if (prescription.getDrugInPrescriptions() != null) {
                for (DrugInPrescription dip : prescription.getDrugInPrescriptions()) {

                    // 🔹 Lấy tên thuốc từ String drugName (có thể null)
                    String drugName = dip.getDrugName();

                    // Nếu bạn muốn bỏ qua thuốc không có tên:
                    // if (drugName == null || drugName.isBlank()) continue;

                    // Hoặc set tên mặc định nếu null/rỗng:
                    if (drugName == null || drugName.isBlank()) {
                        drugName = "Thuốc không tên";
                    }

                    // 🔹 Tìm giờ uống gần nhất (sắp tới so với hiện tại)
                    LocalDateTime nearest = null;
                    if (dip.getSchedules() != null && !dip.getSchedules().isEmpty()) {
                        nearest = dip.getSchedules().stream()
                                .map(Schedule::getDate)
                                .filter(date -> date.isAfter(now))
                                .sorted()
                                .findFirst()
                                .orElse(null);
                    }

                    drugSummaries.add(new DrugSummaryResponse(drugName, nearest));
                }
            }

            PrescriptionSummaryResponse summary = new PrescriptionSummaryResponse(
                    prescription.getId(),
                    prescription.getName(),
                    prescription.getDrugInPrescriptions() != null
                            ? prescription.getDrugInPrescriptions().size()
                            : 0,
                    drugSummaries
            );

            result.add(summary);
        }

        return result;
    }




    @Override
    @Transactional
    public PrescriptionRequest updatePrescription(Long id, PrescriptionRequest request, User user) {
        // 🔹 1. Tìm đơn thuốc theo ID
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn thuốc với ID: " + id));

        // 🔹 2. Kiểm tra quyền sở hữu
        if (user == null || !prescription.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền cập nhật đơn thuốc này");
        }

        // 🔹 3. Validate danh sách thuốc (tùy bạn có muốn cho phép đơn rỗng hay không)
        if (request.getDrugs() == null || request.getDrugs().isEmpty()) {
            throw new IllegalArgumentException("Đơn thuốc phải chứa ít nhất một thuốc");
        }

        // 🔹 4. Cập nhật thông tin cơ bản của đơn thuốc
        prescription.setName(request.getName());
        prescription.setHospital(request.getHospital());
        prescription.setDoctorName(request.getDoctorName());
        prescription.setConsultationDate(parseLocalDate(request.getConsultationDate()));
        prescription.setFollowUpDate(parseLocalDate(request.getFollowUpDate()));
        prescription.setUpdatedAt(LocalDateTime.now());

        // 🔹 5. Xóa dữ liệu thuốc + schedule cũ
        if (prescription.getDrugInPrescriptions() != null) {
            for (DrugInPrescription oldDrug : prescription.getDrugInPrescriptions()) {
                // nếu Schedule có cascade orphanRemoval thì phần này có thể bỏ,
                // nhưng giữ lại cho chắc chắn:
                scheduleRepository.deleteAll(oldDrug.getSchedules());
            }
            drugInPrescriptionRepository.deleteAll(prescription.getDrugInPrescriptions());
            prescription.getDrugInPrescriptions().clear();
        }

        // 🔹 6. Thêm lại danh sách thuốc mới
        for (DrugInPresRequest drugReq : request.getDrugs()) {

            // unitId vẫn bắt buộc
            if (drugReq.getUnitId() == null) {
                throw new IllegalArgumentException("Unit ID must not be null");
            }

            Unit unit = unitRepository.findById(drugReq.getUnitId())
                    .orElseThrow(() -> new RuntimeException(
                            "Không tìm thấy đơn vị với ID: " + drugReq.getUnitId()
                    ));

            if (drugReq.getStartDate() == null || drugReq.getStartDate().isBlank()) {
                throw new IllegalArgumentException("Start date must not be null");
            }

            LocalDate startDate = parseLocalDate(drugReq.getStartDate());
            LocalDate endDate = (drugReq.getEndDate() != null && !drugReq.getEndDate().isBlank())
                    ? parseLocalDate(drugReq.getEndDate())
                    : startDate.plusDays(7);    // default 7 ngày nếu không truyền

            DrugInPrescription newDrug = new DrugInPrescription();
            newDrug.setPrescription(prescription);

            // 🔹 Dùng String drugName (có thể null nếu bạn cho phép)
            newDrug.setDrugName(drugReq.getDrugName());

            newDrug.setUnit(unit);
            newDrug.setStartDate(startDate);
            newDrug.setEndDate(endDate);
            newDrug.setNote(drugReq.getNote());

            // 🔹 Tần suất (mặc định DAILY nếu null)
            FrequencyType frequencyType =
                    (drugReq.getFrequencyType() != null) ? drugReq.getFrequencyType() : FrequencyType.DAILY;
            newDrug.setFrequencyType(frequencyType);

            // INTERVAL → lưu intervalDays, còn lại cho null cho sạch
            if (frequencyType == FrequencyType.INTERVAL) {
                newDrug.setIntervalDays(drugReq.getIntervalDays());
            } else {
                newDrug.setIntervalDays(null);
            }

            // WEEKLY → lưu daysOfWeek, ngoài ra để list rỗng
            if (frequencyType == FrequencyType.WEEKLY &&
                    drugReq.getDaysOfWeek() != null &&
                    !drugReq.getDaysOfWeek().isEmpty()) {
                newDrug.setDaysOfWeek(new ArrayList<>(drugReq.getDaysOfWeek()));
            } else {
                newDrug.setDaysOfWeek(new ArrayList<>());
            }

            // Lưu thuốc mới
            DrugInPrescription savedDrug = drugInPrescriptionRepository.save(newDrug);

            // 🔹 Sinh lại lịch uống dựa theo logic giống createPrescription()
            List<Schedule> newSchedules = generateSchedules(drugReq, savedDrug);
            scheduleRepository.saveAll(newSchedules);

            // Gắn lại vào prescription
            prescription.getDrugInPrescriptions().add(savedDrug);
        }

        // 🔹 7. Lưu lại đơn thuốc
        prescriptionRepository.save(prescription);

        // Ở đây bạn có thể map Prescription → PrescriptionRequest/Response nếu muốn
        return request;
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
                    .drugName(dip.getDrugName() != null ? dip.getDrugName() : null)
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
    private ScheduleResponseDTO toScheduleDTO(Schedule schedule) {

        ScheduleResponseDTO dto = new ScheduleResponseDTO();

        // ====== SCHEDULE ======
        
        dto.setScheduleId(schedule.getId());
        dto.setDosage(schedule.getDosage());
        dto.setStatus(schedule.getStatus());
        dto.setEdited(schedule.isEditted());

        if (schedule.getDate() != null) {
            dto.setTime(schedule.getDate().toLocalTime().toString());
        }

        // ====== DRUG IN PRESCRIPTION ======
        DrugInPrescription dip = schedule.getDrugInPrescription();
        if (dip == null) {
            return dto; // cực hiếm, nhưng phòng thủ
        }
        dto.setId(dip.getId());
        dto.setDrugName(dip.getDrugName());
        dto.setNote(dip.getNote());

        // Unit
        if (dip.getUnit() != null) {
            dto.setUnitName(dip.getUnit().getName());
        }

        // Prescription (nullable là hợp lệ)
        if (dip.getPrescription() != null) {
            dto.setPrescriptionName(dip.getPrescription().getName());
        }

        // ====== TẦN SUẤT UỐNG THUỐC ======
        dto.setFrequencyType(dip.getFrequencyType());
        dto.setIntervalDays(dip.getIntervalDays());
        dto.setDaysOfWeek(dip.getDaysOfWeek());

        return dto;
    }




    @Override
    public Object getSchedulesByDate(LocalDate date, User user) {

        if (date.isBefore(LocalDate.now())) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Ngày bạn chọn đã ở trong quá khứ, không có liều uống nào.");
            return response;
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);

        List<Schedule> schedules = scheduleRepository.findByDateBetween(start, end);

        List<Schedule> filter = schedules.stream()
                .filter(s -> {
                    DrugInPrescription dip = s.getDrugInPrescription();
                    if (dip == null) return true;

                    Prescription p = dip.getPrescription();
                    if (p == null) return true;

                    User u = p.getUser();
                    return u != null
                            && u.getId().equals(user.getId())
                            && Integer.valueOf(1).equals(p.getStatus());
                })
                .sorted(Comparator.comparing(s -> s.getDate().toLocalTime()))
                .toList();


        List<ScheduleResponseDTO> dtoList = filter.stream()
                .map(this::toScheduleDTO)
                .toList();

        return dtoList;
    }
    private boolean isOwner(Schedule schedule, User user) {

        DrugInPrescription dip = schedule.getDrugInPrescription();
        if (dip == null) return false;

        // ✔ Có prescription → check theo prescription
        if (dip.getPrescription() != null) {
            return dip.getPrescription().getUser() != null
                    && dip.getPrescription().getUser().getId().equals(user.getId());
        }

        // ✔ Thuốc đơn → check theo dip.user
        return dip.getUser() != null
                && dip.getUser().getId().equals(user.getId());
    }

    @Override
    @Transactional
    public Object updateScheduleStatus(UpdateScheduleStatusRequest request, User user) {

        Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy schedule"));

        // 🔒 CHECK QUYỀN – HỖ TRỢ CẢ THUỐC ĐƠN & THUỐC THEO ĐƠN
        if (!isOwner(schedule, user)) {
            return Map.of("message", "Bạn không có quyền cập nhật lịch uống này.");
        }

        // ✔ Luôn set editted
        schedule.setEditted(true);

        // STATUS = 0 (KHÔNG UỐNG)
        if (request.getStatus() == 0) {
            schedule.setStatus(0);
            scheduleRepository.save(schedule);
            return Map.of("message", "Đã cập nhật: Không uống thuốc.");
        }

        // STATUS = 1 (CÓ UỐNG)
        if (request.getStatus() == 1) {

            LocalDateTime scheduleTime = schedule.getDate();
            LocalDateTime now = LocalDateTime.now();

            if (scheduleTime != null && now.isAfter(scheduleTime.plusMinutes(10))) {
                schedule.setStatus(2); // uống trễ
            } else {
                schedule.setStatus(1); // đúng giờ
            }

            scheduleRepository.save(schedule);
            return Map.of("message", "Đã xác nhận uống thuốc.");
        }

        return Map.of("message", "Trạng thái không hợp lệ.");
    }

    @Override
    public Object getHistory(User user, String filter, Integer year, Integer month) {

        LocalDate today = LocalDate.now();

        List<Schedule> baseSchedules =
                scheduleRepository.findByEdittedTrue();

        List<Schedule> schedules = baseSchedules.stream()
                .filter(s -> {
                    DrugInPrescription dip = s.getDrugInPrescription();
                    if (dip == null) return true;

                    Prescription p = dip.getPrescription();
                    if (p == null) return true;

                    return p.getUser() != null
                            && p.getUser().getId().equals(user.getId());
                })
                .toList();

        // ================================
        // 🔍 FILTER THEO THỜI GIAN
        // ================================
        if ("7days".equalsIgnoreCase(filter)) {
            LocalDate start = today.minusDays(7);

            schedules = schedules.stream()
                    .filter(s -> {
                        LocalDate d = s.getDate().toLocalDate();
                        return !d.isBefore(start) && !d.isAfter(today);
                    })
                    .toList();
        }

        else if ("month".equalsIgnoreCase(filter) && year != null && month != null) {
            schedules = schedules.stream()
                    .filter(s -> {
                        LocalDate d = s.getDate().toLocalDate();
                        return d.getYear() == year && d.getMonthValue() == month;
                    })
                    .toList();
        }


        // GROUP THEO NGÀY

        Map<LocalDate, List<Schedule>> grouped =
                schedules.stream()
                        .collect(Collectors.groupingBy(
                                s -> s.getDate().toLocalDate()
                        ));

        List<ScheduleHistoryDTO> result = grouped.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, List<Schedule>>comparingByKey().reversed())
                .map(entry -> {

                    List<ScheduleResponseDTO> list = entry.getValue().stream()
                            .sorted(Comparator.comparing(s -> s.getDate().toLocalTime()))
                            .map(this::toScheduleDTO)
                            .toList();

                    return new ScheduleHistoryDTO(entry.getKey(), list);
                })
                .toList();

        // ================================
        // 📊 THỐNG KÊ
        // ================================
        long total = schedules.size();
        long onTime = schedules.stream().filter(s -> s.getStatus() == 1).count();
        long late = schedules.stream().filter(s -> s.getStatus() == 2).count();
        long skipped = schedules.stream().filter(s -> s.getStatus() == 0).count();

        return Map.of(
                "history", result,
                "statistics", Map.of(
                        "totalTaken", total,
                        "onTime", onTime,
                        "late", late,
                        "skipped", skipped
                )
        );
    }

    @Transactional
    public void deleteAllPrescriptionsForTesting() {

        // ⚠️ Cẩn thận: XÓA TOÀN BỘ dữ liệu liên quan đến đơn thuốc trong DB

        // 1) Xoá hết Schedule
        scheduleRepository.deleteAll();

        // 2) Xoá hết DrugInPrescription
        drugInPrescriptionRepository.deleteAll();

        // 3) Xoá hết Prescription
        prescriptionRepository.deleteAll();
    }
    @Override
    @Transactional
    public DrugInPrescription createSingleDrug(DrugInPresRequest drugReq, User user) {

        if (user == null) {
            throw new IllegalArgumentException("User must not be null when creating single drug");
        }

        if (drugReq.getUnitId() == null) {
            throw new IllegalArgumentException("Unit ID must not be null");
        }

        Unit unit = unitRepository.findById(drugReq.getUnitId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Unit not found with ID: " + drugReq.getUnitId())
                );

        if (drugReq.getStartDate() == null || drugReq.getStartDate().isBlank()) {
            throw new IllegalArgumentException("Start date must not be null");
        }

        LocalDate startDate = parseLocalDate(drugReq.getStartDate());
        LocalDate endDate = (drugReq.getEndDate() != null && !drugReq.getEndDate().isBlank())
                ? parseLocalDate(drugReq.getEndDate())
                : startDate.plusDays(7);

        DrugInPrescription drugInPrescription = new DrugInPrescription();

        // ❌ KHÔNG gắn prescription
        drugInPrescription.setPrescription(null);

        // ✅ GẮN user – rất quan trọng
        drugInPrescription.setUser(user);

        drugInPrescription.setDrugName(drugReq.getDrugName());
        drugInPrescription.setUnit(unit);
        drugInPrescription.setStartDate(startDate);
        drugInPrescription.setEndDate(endDate);
        drugInPrescription.setNote(drugReq.getNote());

        FrequencyType frequencyType =
                (drugReq.getFrequencyType() != null) ? drugReq.getFrequencyType() : FrequencyType.DAILY;
        drugInPrescription.setFrequencyType(frequencyType);

        if (frequencyType == FrequencyType.INTERVAL) {
            drugInPrescription.setIntervalDays(drugReq.getIntervalDays());
        } else {
            drugInPrescription.setIntervalDays(null);
        }

        if (frequencyType == FrequencyType.WEEKLY &&
                drugReq.getDaysOfWeek() != null &&
                !drugReq.getDaysOfWeek().isEmpty()) {
            drugInPrescription.setDaysOfWeek(new ArrayList<>(drugReq.getDaysOfWeek()));
        } else {
            drugInPrescription.setDaysOfWeek(new ArrayList<>());
        }

        DrugInPrescription savedDrugInPres = drugInPrescriptionRepository.save(drugInPrescription);

        List<Schedule> generatedSchedules = generateSchedules(drugReq, savedDrugInPres);
        scheduleRepository.saveAll(generatedSchedules);

        return savedDrugInPres;
    }

    @Override
    @Transactional
    public DrugInPrescription updateDrug(Long drugInPresId,
                                         DrugInPresRequest drugReq,
                                         User user) {

        if (user == null) {
            throw new IllegalArgumentException("User must not be null when updating drug");
        }

        // 1. Lấy thuốc
        DrugInPrescription drug = drugInPrescriptionRepository.findById(drugInPresId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "DrugInPrescription not found with ID: " + drugInPresId
                ));

        // 2. Check quyền (dựa vào drug.user)
        if (drug.getUser() == null || !drug.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied for this drug");
        }

        // 3. Validate & load Unit
        if (drugReq.getUnitId() == null) {
            throw new IllegalArgumentException("Unit ID must not be null");
        }

        Unit unit = unitRepository.findById(drugReq.getUnitId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Unit not found with ID: " + drugReq.getUnitId())
                );

        // 4. Validate ngày
        if (drugReq.getStartDate() == null || drugReq.getStartDate().isBlank()) {
            throw new IllegalArgumentException("Start date must not be null");
        }

        LocalDate startDate = parseLocalDate(drugReq.getStartDate());
        LocalDate endDate = (drugReq.getEndDate() != null && !drugReq.getEndDate().isBlank())
                ? parseLocalDate(drugReq.getEndDate())
                : startDate.plusDays(7);

        // 5. Cập nhật field của DrugInPrescription
        drug.setDrugName(drugReq.getDrugName());
        drug.setUnit(unit);
        drug.setStartDate(startDate);
        drug.setEndDate(endDate);
        drug.setNote(drugReq.getNote());

        FrequencyType frequencyType =
                (drugReq.getFrequencyType() != null) ? drugReq.getFrequencyType() : FrequencyType.DAILY;
        drug.setFrequencyType(frequencyType);

        if (frequencyType == FrequencyType.INTERVAL) {
            drug.setIntervalDays(drugReq.getIntervalDays());
        } else {
            drug.setIntervalDays(null);
        }

        if (frequencyType == FrequencyType.WEEKLY &&
                drugReq.getDaysOfWeek() != null &&
                !drugReq.getDaysOfWeek().isEmpty()) {
            drug.setDaysOfWeek(new ArrayList<>(drugReq.getDaysOfWeek()));
        } else {
            drug.setDaysOfWeek(new ArrayList<>());
        }

        // 6. XÓA LỊCH CŨ
        // do bạn đã set orphanRemoval = true, chỉ cần clear list là DB cũng xoá
        drug.getSchedules().clear();

        // 7. Generate lịch mới dùng lại hàm generateSchedules cũ
        List<Schedule> newSchedules = generateSchedules(drugReq, drug);

        // 8. Gắn vào entity (giữ state trong bộ nhớ cho đúng)
        drug.getSchedules().addAll(newSchedules);

        // 9. Lưu schedule (có 2 cách, chọn 1):
        // Cách 1: rely vào cascade, không cần saveAll, chỉ cần drug đang managed:
        // -> JPA sẽ tự persist newSchedules khi transaction commit.
        // return drug;

        // Cách 2: explicit:
        scheduleRepository.saveAll(newSchedules);

        return drug;
    }

    // ================== DELETE THUỐC ==================
    @Override
    @Transactional
    public void deleteDrug(Long drugInPresId, User user) {

        if (user == null) {
            throw new IllegalArgumentException("User must not be null when deleting drug");
        }

        DrugInPrescription drug = drugInPrescriptionRepository.findById(drugInPresId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "DrugInPrescription not found with ID: " + drugInPresId
                ));

        if (drug.getUser() == null || !drug.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied for this drug");
        }

        // Chỉ cần delete drug, nhờ orphanRemoval nên schedules sẽ bị xoá theo
        drugInPrescriptionRepository.delete(drug);
    }

    @Transactional
    @Override
    public DrugInPrescription toggleSingleDrugStatus(Long id, User user) {

        DrugInPrescription dip = drugInPrescriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc trong đơn"));

        // Kiểm tra quyền sở hữu
        if (!dip.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền thay đổi trạng thái đơn thuốc này");
        }

        // Đảo trạng thái: 1 -> 0, 0 -> 1
        dip.setStatus(
                Integer.valueOf(1).equals(dip.getStatus()) ? 0 : 1
        );

        return drugInPrescriptionRepository.save(dip);
    }

    @Override
    public List<SingleDrugResponse> getSingleDrugs(User user, Integer status) {

        List<DrugInPrescription> drugs =
                drugInPrescriptionRepository
                        .findByUserAndPrescriptionIsNullAndStatus(user, status);

        return drugs.stream()
                .map(dip -> {

                    String drugName = dip.getDrugName();
                    if (drugName == null || drugName.isBlank()) {
                        drugName = "Thuốc không tên";
                    }

                    String unitName = dip.getUnit() != null
                            ? dip.getUnit().getName()
                            : null;

                    // 🔥 LẤY GIỜ KHÔNG TRÙNG
                    List<DrugScheduleResponse> schedules =
                            dip.getSchedules() == null
                                    ? List.of()
                                    : dip.getSchedules().stream()
                                    .collect(Collectors.toMap(
                                            s -> s.getDate().toLocalTime(), // key = giờ
                                            s -> new DrugScheduleResponse(
                                                    s.getDate().toLocalTime(),
                                                    s.getDosage(),
                                                    unitName
                                            ),
                                            (existing, ignore) -> existing // trùng giờ → giữ 1
                                    ))
                                    .values()
                                    .stream()
                                    .sorted(Comparator.comparing(DrugScheduleResponse::getTime))
                                    .toList();

                    return new SingleDrugResponse(
                            dip.getId(),
                            drugName,
                            dip.getNote(),
                            dip.getFrequencyType(),
                            dip.getIntervalDays(),
                            dip.getDaysOfWeek(),
                            schedules
                    );
                })
                .toList();
    }




    public DrugInPresRequest mapDrugInPrescriptionToRequest(DrugInPrescription dip) {

        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        DrugInPresRequest dReq = DrugInPresRequest.builder()
                .drugName(dip.getDrugName())
                .unitId(dip.getUnit() != null ? dip.getUnit().getId() : null)
                .startDate(dip.getStartDate() != null
                        ? dip.getStartDate().format(dateFormatter)
                        : null)
                .endDate(dip.getEndDate() != null
                        ? dip.getEndDate().format(dateFormatter)
                        : null)
                .note(dip.getNote())
                .frequencyType(dip.getFrequencyType())
                .intervalDays(dip.getIntervalDays())
                .daysOfWeek(dip.getDaysOfWeek() != null
                        ? new ArrayList<>(dip.getDaysOfWeek())
                        : new ArrayList<>())
                .schedules(new ArrayList<>())
                .build();

        // gộp schedule theo giờ
        Map<LocalTime, Double> timeToDosage = new HashMap<>();
        if (dip.getSchedules() != null) {
            for (Schedule s : dip.getSchedules()) {
                if (s == null || s.getDate() == null) continue;
                LocalTime time = s.getDate().toLocalTime();
                timeToDosage.putIfAbsent(time, s.getDosage());
            }
        }

        List<ScheduleAddRequest> schedules = timeToDosage.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    ScheduleAddRequest sa = new ScheduleAddRequest();
                    sa.setTime(e.getKey().format(timeFormatter));
                    sa.setDosage(e.getValue() != null ? e.getValue() : 1.0);
                    return sa;
                })
                .toList();

        dReq.setSchedules(new ArrayList<>(schedules));

        return dReq;
    }
    @Transactional(readOnly = true)
    @Override
    public DrugInPresRequest getSingleDrugAsRequest(Long drugId, User user) {

        DrugInPrescription dip = drugInPrescriptionRepository.findById(drugId)
                .orElseThrow(() -> new RuntimeException("Drug not found"));

        // check owner
        if (!dip.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        return mapDrugInPrescriptionToRequest(dip);
    }



}
