package com.example.AuthService.service.impl;

import com.example.AuthService.entity.Drug;
import com.example.AuthService.repository.DrugRepository;
import com.example.AuthService.service.DrugService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DrugServiceImpl implements DrugService {
    private final DrugRepository drugRepository;

    @Override
    public Drug createDrug(Drug drug) {
        return drugRepository.save(drug);
    }

    @Override
    public Drug updateDrug(Long id, Drug updated) {
        Drug drug = drugRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Drug not found"));
        drug.setName(updated.getName());
        drug.setTitle(updated.getTitle());
        drug.setImage(updated.getImage());
        drug.setPrice(updated.getPrice());
        drug.setStockQuantity(updated.getStockQuantity());
        return drugRepository.save(drug);
    }

    @Override
    public void deleteDrug(Long id) {
        if (!drugRepository.existsById(id)) {
            throw new RuntimeException("Drug not found");
        }
        drugRepository.deleteById(id);
    }

    @Override
    public Drug getDrugById(Long id) {
        return drugRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Drug not found"));
    }

    @Override
    public List<Drug> getAllDrugs() {
        return drugRepository.findAll();
    }
}
