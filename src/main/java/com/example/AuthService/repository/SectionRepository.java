package com.example.AuthService.repository;

import com.example.AuthService.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {

    // Lấy tất cả section của 1 thuốc, sắp xếp theo id tăng dần
    List<Section> findByDrugIdOrderByIdAsc(Long drugId);

    // (tuỳ chọn) Lấy tất cả sections, sắp xếp theo id tăng dần
    List<Section> findAllByOrderByIdAsc();
}
