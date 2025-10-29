package com.example.AuthService.repository;

import com.example.AuthService.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);            // "USER" | "MODERATOR" | "ADMIN"
    Optional<Role> findByNameIgnoreCase(String name);  // tiện khi nhập thường/hoa
    boolean existsByName(String name);
}
