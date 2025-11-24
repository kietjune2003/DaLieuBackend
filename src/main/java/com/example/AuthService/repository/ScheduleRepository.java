package com.example.AuthService.repository;

import com.example.AuthService.entity.DrugInPrescription;
import com.example.AuthService.entity.Prescription;
import com.example.AuthService.entity.Schedule;
import com.example.AuthService.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByDrugInPrescription(DrugInPrescription dip);
    List<Schedule> findByDateBetween(LocalDateTime start, LocalDateTime end);
    List<Schedule> findByEdittedTrueAndDrugInPrescription_Prescription_User(User user);

}
