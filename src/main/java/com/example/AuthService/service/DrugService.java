package com.example.AuthService.service;

import com.example.AuthService.dto.DrugFilter;
import com.example.AuthService.entity.Drug;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DrugService {
    Drug createDrug(Drug drug);

    Drug createDrugWithImage(Drug drug, MultipartFile image);




    Drug updateDrugWithImage(Long id, Drug updated, MultipartFile image);

    void deleteDrug(Long id);
    Drug getDrugById(Long id);
    List<Drug> getAllDrugs();

    // Phân trang + lọc (CHÚ Ý: DrugFilter trước, Pageable sau)
    Page<Drug> getDrugs(DrugFilter filter, Pageable pageable);

    // Gợi ý tên
    List<String> suggestNames(String q, int limit);
}
