package com.example.AuthService.controller;

import com.example.AuthService.dto.request.DrugInPresRequest;
import com.example.AuthService.dto.request.PrescriptionRequest;
import com.example.AuthService.dto.request.UpdateScheduleStatusRequest;
import com.example.AuthService.entity.DrugInPrescription;
import com.example.AuthService.entity.Prescription;
import com.example.AuthService.entity.User;
import com.example.AuthService.repository.UserRepository;
import com.example.AuthService.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final UserRepository userRepository;

    // ✅ 1. Tạo đơn thuốc (đã có)
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createPrescription(
            @RequestBody PrescriptionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + email));

        Prescription savedPrescription = prescriptionService.createPrescription(request, user);
        return ResponseEntity.ok("✅ Đã tạo đơn thuốc thành công! ID: " + savedPrescription.getId());
    }

    // 🗑 2. Xóa đơn thuốc (xoá cả bảng con)
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deletePrescription(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + email));

        prescriptionService.deletePrescription(id, user);
        return ResponseEntity.ok("🗑️ Đã xoá đơn thuốc thành công!");
    }

    // 🔍 3. Lấy danh sách theo trạng thái
    @GetMapping("/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPrescriptionsByStatus(
            @PathVariable Integer status,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + email));

        return ResponseEntity.ok(prescriptionService.getPrescriptionsByStatus(user, status));
    }

    // ✏️ 4. Cập nhật đơn thuốc (tên, bệnh viện, bác sĩ, ngày tái khám)
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updatePrescription(
            @PathVariable Long id,
            @RequestBody PrescriptionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + email));

        PrescriptionRequest updated = prescriptionService.updatePrescription(id, request, user);
        return ResponseEntity.ok(updated);
    }

    // 📄 5. Lấy chi tiết đơn thuốc theo ID
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPrescriptionAsRequestById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + email));

        PrescriptionRequest prescription = prescriptionService.getPrescriptionAsRequestById(id, user);
        return ResponseEntity.ok(prescription);
    }
    // 🔄 6. Đổi trạng thái đơn thuốc (1 -> 0 hoặc 0 -> 1)
    @PutMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> togglePrescriptionStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + email));

        Prescription updated = prescriptionService.togglePrescriptionStatus(id, user);
        return ResponseEntity.ok("✅ Đã thay đổi trạng thái đơn thuốc ID " + id + " → status = " + updated.getStatus());
    }
    // 📆 7. Lấy danh sách liều uống theo ngày
    @GetMapping("/schedules")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSchedulesByDate(
            @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + email));

        return ResponseEntity.ok(prescriptionService.getSchedulesByDate(date, user));
    }
    @PutMapping("/schedules/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateScheduleStatus(
            @RequestBody UpdateScheduleStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + email));

        return ResponseEntity.ok(prescriptionService.updateScheduleStatus(request, user));
    }
    @GetMapping("/schedules/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getScheduleHistory(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + email));

        return ResponseEntity.ok(prescriptionService.getHistory(user, filter, year, month));
    }
    @DeleteMapping("/clear-all")
    public Map<String, String> clearAll() {
        prescriptionService.deleteAllPrescriptionsForTesting();
        return Map.of("message", "Đã xoá toàn bộ dữ liệu đơn thuốc (prescription, drug_in_prescriptions, schedules).");
    }

    // ✅ 1. Tạo đơn thuốc (đã có)
    @PostMapping("/single-drug")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createSingleDrug(
            @RequestBody DrugInPresRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + email));

        DrugInPrescription savedDruginpres = prescriptionService.createSingleDrug(request, user);
        return ResponseEntity.ok("✅ Đã tạo  thuốc lẻ thành công! ID: " + savedDruginpres.getId());
    }
    // ✏️ Cập nhật thuốc (thuốc đơn hoặc thuốc trong đơn)
    @PutMapping("/drugs/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateDrug(
            @PathVariable("id") Long drugInPresId,
            @RequestBody DrugInPresRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Lấy user từ email giống các API khác
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + email));

        DrugInPrescription updated = prescriptionService.updateDrug(drugInPresId, request, user);

        // Trả về message đơn giản (đúng style create/delete prescription)
        return ResponseEntity.ok("✅ Đã cập nhật thuốc thành công! ID: " + updated.getId());
    }

    // 🗑 Xoá thuốc (thuốc đơn hoặc thuốc trong đơn)
    @DeleteMapping("/drugs/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteDrug(
            @PathVariable("id") Long drugInPresId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + email));

        prescriptionService.deleteDrug(drugInPresId, user);

        return ResponseEntity.ok("🗑️ Đã xoá thuốc thành công!");
    }
    //  Lấy danh sách thuốc lẻ theo trạng thái
    @GetMapping("/single-drug/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSingleDrugs(
            @PathVariable Integer status,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + email));

        return ResponseEntity.ok(prescriptionService.getSingleDrugs(user, status));
    }
    // Đổi trạng thái thuốc lẻ (1 -> 0 hoặc 0 -> 1)
    @PutMapping("/single-drug/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> toggleSingleDrugStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + email));

        DrugInPrescription updated = prescriptionService.toggleSingleDrugStatus(id, user);
        return ResponseEntity.ok(" Đã thay đổi trạng thái đơn thuốc ID " + id + " → status = " + updated.getStatus());
    }
    @GetMapping("/single-drug/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSingleDrugDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(
                prescriptionService.getSingleDrugAsRequest(id, user)
        );
    }

}
