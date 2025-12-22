package com.example.AuthService.service;

import com.example.AuthService.dto.request.DrugInPresRequest;
import com.example.AuthService.dto.request.UpdateScheduleStatusRequest;
import com.example.AuthService.dto.response.PrescriptionSummaryResponse;
import com.example.AuthService.dto.response.SingleDrugResponse;
import com.example.AuthService.entity.DrugInPrescription;
import com.example.AuthService.entity.Prescription;
import com.example.AuthService.entity.User;
import com.example.AuthService.dto.request.PrescriptionRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface PrescriptionService {
    Prescription createPrescription(PrescriptionRequest request, User user);
    void deletePrescription(Long id, User user);
    List<PrescriptionSummaryResponse> getPrescriptionsByStatus(User user, Integer status);
    PrescriptionRequest updatePrescription(Long id, PrescriptionRequest request, User user);
    PrescriptionRequest getPrescriptionAsRequestById(Long id, User user);


    Prescription togglePrescriptionStatus(Long id, User user);
    Object getSchedulesByDate(LocalDate date, User user);
    Object updateScheduleStatus(UpdateScheduleStatusRequest request, User user);
    Object getHistory(User user, String filter, Integer year, Integer month);
    DrugInPrescription createSingleDrug(DrugInPresRequest request, User user);
    void deleteAllPrescriptionsForTesting();
    // 🔹 Cập nhật thuốc (đơn hoặc trong đơn)
    DrugInPrescription updateDrug(Long drugInPresId, DrugInPresRequest request, User user);

    // 🔹 Xoá thuốc (đơn hoặc trong đơn)
    void deleteDrug(Long drugInPresId, User user);

    @Transactional
    DrugInPrescription toggleSingleDrugStatus(Long id, User user);

    List<SingleDrugResponse> getSingleDrugs(User user, Integer status);

    DrugInPresRequest mapDrugInPrescriptionToRequest(DrugInPrescription dip);

    @Transactional(readOnly = true)
    DrugInPresRequest getSingleDrugAsRequest(Long drugId, User user);
}
