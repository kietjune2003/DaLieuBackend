package com.example.AuthService.service;

import com.example.AuthService.dto.response.PrescriptionSummaryResponse;
import com.example.AuthService.entity.Prescription;
import com.example.AuthService.entity.User;
import com.example.AuthService.dto.request.PrescriptionRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PrescriptionService {
    Prescription createPrescription(PrescriptionRequest request, User user);
    void deletePrescription(Long id, User user);
    List<PrescriptionSummaryResponse> getPrescriptionsByStatus(User user, Integer status);
    PrescriptionRequest updatePrescription(Long id, PrescriptionRequest request, User user);
    PrescriptionRequest getPrescriptionAsRequestById(Long id, User user);

    @Transactional
    Prescription togglePrescriptionStatus(Long id, User user);
}
