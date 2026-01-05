package com.example.AuthService.repository;

import com.example.AuthService.entity.DrugInPrescription;
import com.example.AuthService.entity.Prescription;
import com.example.AuthService.entity.Schedule;
import com.example.AuthService.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByDrugInPrescription(DrugInPrescription dip);
    List<Schedule> findByDateBetween(LocalDateTime start, LocalDateTime end);
    List<Schedule> findByEdittedTrueAndDrugInPrescription_Prescription_User(User user);
    void deleteByDrugInPrescription(DrugInPrescription drugInPrescription);

    List<Schedule> findByEdittedTrue();
    @Modifying
    @Query("""
        UPDATE Schedule s
        SET s.editted = true
        WHERE s.date < :todayStart
          AND s.editted = false
    """)
    int autoMarkSkipped(@Param("todayStart") LocalDateTime todayStart);

    @Query("""
    SELECT MAX(s.date)
    FROM Schedule s
    WHERE s.drugInPrescription.id = :dipId
    """)
    LocalDateTime findLastScheduleDate(@Param("dipId") Long dipId);
}
