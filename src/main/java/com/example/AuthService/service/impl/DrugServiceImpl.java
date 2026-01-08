package com.example.AuthService.service.impl;

import com.example.AuthService.dto.DrugFilter;
import com.example.AuthService.dto.response.DrugResponse;
import com.example.AuthService.entity.Drug;
import com.example.AuthService.repository.DrugRepository;
import com.example.AuthService.service.CloudinaryService;
import com.example.AuthService.service.DrugService;


import com.example.AuthService.service.InventoryService;
import com.example.AuthService.spec.DrugSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DrugServiceImpl implements DrugService {
    private final DrugRepository drugRepository;
    private final CloudinaryService cloudinaryService;
    private final InventoryService inventoryService;
    @Override
    public Drug createDrug(Drug drug) {
        if (drug.getSections() != null) {
            drug.getSections().forEach(section -> section.setDrug(drug));
        }
        return drugRepository.save(drug);
    }
    @Override
    public Drug createDrugWithImage(Drug drug, MultipartFile image) {

        // 1️⃣ Upload ảnh
        String imageUrl = cloudinaryService.uploadImage(image);
        drug.setImage(imageUrl);

        // 2️⃣ Set quan hệ Section
        if (drug.getSections() != null) {
            drug.getSections().forEach(section -> section.setDrug(drug));
        }

        // 3️⃣ Save DB
        return drugRepository.save(drug);
    }

    @Override
    public Drug updateDrugActive(Long id, boolean active) {
        Drug drug = drugRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc với id = " + id));

        drug.setIsActive(active);

        return drugRepository.save(drug);
    }

    @Override
    public Drug updateDrugWithImage(Long id, Drug updated, MultipartFile image) {

        Drug drug = drugRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Drug not found"));

        // 1️⃣ Nếu có ảnh mới → upload
        if (image != null && !image.isEmpty()) {
            String imageUrl = cloudinaryService.uploadImage(image);
            drug.setImage(imageUrl);
        }

        // 2️⃣ Update field cơ bản
        drug.setName(updated.getName());
        drug.setTitle(updated.getTitle());
        drug.setPrice(updated.getPrice());
        drug.setImportPrice(updated.getImportPrice());
//        drug.setStockQuantity(updated.getStockQuantity());

        // 3️⃣ Update sections (nếu có)
        if (updated.getSections() != null) {
            drug.getSections().clear();
            updated.getSections().forEach(section -> {
                section.setDrug(drug);
                drug.getSections().add(section);
            });
        }

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

    @Override
    public Page<DrugResponse> getDrugs(
            DrugFilter filter,
            Pageable pageable,
            boolean isAdmin
    ) {
        Pageable effective = (pageable == null || pageable.getSort().isUnsorted())
                ? PageRequest.of(
                pageable == null ? 0 : pageable.getPageNumber(),
                pageable == null ? 20 : pageable.getPageSize(),
                Sort.by(Sort.Order.desc("id"))
        )
                : pageable;

        if (!isAdmin) {
            filter.setIsActive(true);
        }

        Specification<Drug> spec = DrugSpecifications.withFilter(filter);

        Page<Drug> drugPage =
                drugRepository.findAll(spec, effective);

        List<Long> drugIds = drugPage.getContent()
                .stream()
                .map(Drug::getId)
                .toList();

        Map<Long, Integer> importedMap =
                inventoryService.getTotalImportedForDrugs(drugIds);

        return drugPage.map(drug -> {
            int totalImported =
                    importedMap.getOrDefault(drug.getId(), 0);

            int sold = drug.getSoldQuantity() != null
                    ? drug.getSoldQuantity()
                    : 0;

            int stock = totalImported - sold;

            return DrugResponse.builder()
                    .id(drug.getId())
                    .name(drug.getName())
                    .title(drug.getTitle())
                    .image(drug.getImage())
                    .price(drug.getPrice())
                    .soldQuantity(sold)
                    .importPrice(drug.getImportPrice())
                    .isActive(drug.getIsActive())
                    .stockQuantity(stock)
                    .build();
        });
    }





    @Override
    public List<String> suggestNames(String q, int limit) {
        int size = (limit <= 0 || limit > 20) ? 10 : limit;
        return drugRepository.suggestNames(q == null ? "" : q.trim(), PageRequest.of(0, size));
    }
}
