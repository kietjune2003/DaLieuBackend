package com.example.AuthService.repository;

import com.example.AuthService.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DrugRepository extends JpaRepository<Drug, Long> {}