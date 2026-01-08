package com.example.AuthService.service;

import com.example.AuthService.dto.DrugFilter;
import com.example.AuthService.dto.response.DrugResponse;
import com.example.AuthService.entity.Drug;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DrugService {
    Drug createDrug(Drug drug);

    Drug createDrugWithImage(Drug drug, MultipartFile image);


    Drug updateDrugActive(Long id, boolean active);

    Drug updateDrugWithImage(Long id, Drug updated, MultipartFile image);

    void deleteDrug(Long id);
    Drug getDrugById(Long id);
    List<Drug> getAllDrugs();

    Page<DrugResponse> getDrugs(DrugFilter filter, Pageable pageable, boolean isAdmin);


    // Gợi ý tên
    List<String> suggestNames(String q, int limit);
}
