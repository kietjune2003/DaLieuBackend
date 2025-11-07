package com.example.AuthService.repository;

import com.example.AuthService.entity.DrugInPrescription;
import com.example.AuthService.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrugInPrescriptionRepository extends JpaRepository<DrugInPrescription, Long> {
    List<DrugInPrescription> findByPrescription(Prescription prescription);
}
