package com.example.AuthService.repository;

import com.example.AuthService.entity.DrugInPrescription;
import com.example.AuthService.entity.Prescription;
import com.example.AuthService.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByDrugInPrescription(DrugInPrescription dip);
}
