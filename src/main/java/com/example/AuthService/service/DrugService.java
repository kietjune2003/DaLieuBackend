package com.example.AuthService.service;

import com.example.AuthService.entity.Drug;
import java.util.List;

public interface DrugService {
    Drug createDrug(Drug drug);
    Drug updateDrug(Long id, Drug drug);
    void deleteDrug(Long id);
    Drug getDrugById(Long id);
    List<Drug> getAllDrugs();
}
