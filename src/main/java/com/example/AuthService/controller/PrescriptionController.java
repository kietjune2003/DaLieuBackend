package com.example.AuthService.controller;

import com.example.AuthService.dto.request.PrescriptionRequest;
import com.example.AuthService.entity.Prescription;
import com.example.AuthService.entity.User;
import com.example.AuthService.repository.UserRepository;
import com.example.AuthService.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

}
