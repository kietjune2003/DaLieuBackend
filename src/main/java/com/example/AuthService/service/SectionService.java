package com.example.AuthService.service;

import com.example.AuthService.entity.Section;

import java.util.List;

public interface SectionService {
    List<Section> listByDrug(Long drugId);  // KHÔNG phân trang
    Section create(Long drugId, Section payload);
    Section getById(Long id);
    Section update(Long id, Section payload);
    void delete(Long id);

    // tuỳ chọn: list toàn bộ sections (KHÔNG phân trang)
    List<Section> listAll();
}
