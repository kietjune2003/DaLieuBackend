package com.example.AuthService.service.impl;

import com.example.AuthService.dto.DrugFilter;
import com.example.AuthService.entity.Drug;
import com.example.AuthService.repository.DrugRepository;
import com.example.AuthService.service.DrugService;


import com.example.AuthService.spec.DrugSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
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

    // CHÚ Ý chữ ký phải TRÙNG interface
    @Override
    public Page<Drug> getDrugs(DrugFilter filter, Pageable pageable) {
        Pageable effective = (pageable == null || pageable.getSort().isUnsorted())
                ? PageRequest.of(pageable == null ? 0 : pageable.getPageNumber(),
                pageable == null ? 20 : pageable.getPageSize(),
                Sort.by(Sort.Order.desc("id")))
                : pageable;

        Specification<Drug> spec = DrugSpecifications.withFilter(filter);
        return drugRepository.findAll(spec, effective);
    }

    @Override
    public List<String> suggestNames(String q, int limit) {
        int size = (limit <= 0 || limit > 20) ? 10 : limit;
        return drugRepository.suggestNames(q == null ? "" : q.trim(), PageRequest.of(0, size));
    }
}
