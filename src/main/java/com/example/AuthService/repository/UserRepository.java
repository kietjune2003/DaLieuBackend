package com.example.AuthService.repository;

import com.example.AuthService.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Tìm user theo email (đã chuẩn hoá lowercase ở @PrePersist/@PreUpdate)
    Optional<User> findByEmail(String email);

    // (tiện) tìm mà không phân biệt hoa/thường
    Optional<User> findByEmailIgnoreCase(String email);

    // (tiện) kiểm tra tồn tại email
    boolean existsByEmail(String email);

    // (tuỳ chọn) dùng cho đăng nhập xã hội
    Optional<User> findByGoogleAccountId(String googleAccountId);
    Optional<User> findByFacebookAccountId(String facebookAccountId);
}
