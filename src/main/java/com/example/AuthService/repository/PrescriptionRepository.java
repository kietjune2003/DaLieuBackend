package com.example.AuthService.repository;

import com.example.AuthService.entity.Prescription;
import com.example.AuthService.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByUser(User user);
    List<Prescription> findByUserAndStatus(User user, Integer status);
    Optional<Prescription> findByIdAndUser(Long id, User user);
}
