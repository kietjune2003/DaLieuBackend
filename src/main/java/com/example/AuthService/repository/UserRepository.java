package com.example.AuthService.repository;

import com.example.AuthService.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    // Tìm user theo email (đã chuẩn hoá lowercase ở @PrePersist/@PreUpdate)
    Optional<User> findByEmail(String email);

    // (tiện) tìm mà không phân biệt hoa/thường
    Optional<User> findByEmailIgnoreCase(String email);

    // (tiện) kiểm tra tồn tại email
    boolean existsByEmail(String email);


    Optional<User> findByGoogleAccountId(String googleAccountId);

    Page<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name, String email, Pageable pageable
    );
}
