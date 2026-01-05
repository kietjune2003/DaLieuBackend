package com.example.AuthService.repository;

import com.example.AuthService.entity.DrugInPrescription;
import com.example.AuthService.entity.Prescription;
import com.example.AuthService.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface DrugInPrescriptionRepository extends JpaRepository<DrugInPrescription, Long> {
    List<DrugInPrescription>  findByUser(User user);

    List<DrugInPrescription> findByUserAndPrescriptionIsNullAndStatus(
            User user,
            Integer status
    );
    List<DrugInPrescription> findByPrescription(Prescription prescription);
    List<DrugInPrescription> findByEndDateIsNull();
}
