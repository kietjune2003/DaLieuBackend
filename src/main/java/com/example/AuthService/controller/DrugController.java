package com.example.AuthService.controller;

import com.example.AuthService.dto.DrugFilter; // giữ nguyên import theo file bạn đang dùng
import com.example.AuthService.dto.request.UpdateDrugActiveRequest;
import com.example.AuthService.dto.response.DrugResponse;
import com.example.AuthService.entity.Drug;
import com.example.AuthService.service.DrugService;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/drugs")
@RequiredArgsConstructor
public class DrugController {

    private final DrugService drugService;

    /**
     * Phân trang + lọc + sắp xếp
     * GET /api/drugs?page=0&size=20&sort=id,desc&q=para&minPrice=10000&maxPrice=50000&inStock=true&hasImage=true
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public ResponseEntity<Page<DrugResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) Boolean hasImage,
            @PageableDefault(size = 20, sort = "id") Pageable pageable,
            Authentication authentication
    ) {
        DrugFilter filter = new DrugFilter();
        filter.setQ(q);
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setInStock(inStock);
        filter.setHasImage(hasImage);

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MODERATOR"));

        Page<DrugResponse> drugResponses = drugService.getDrugs(filter, pageable, isAdmin);

        return ResponseEntity.ok(drugResponses);
    }



    /**
     * Gợi ý autocomplete cho ô search
     * GET /api/drugs/suggest?q=para&limit=10
     */
    @GetMapping("/suggest")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public ResponseEntity<List<String>> suggest(@RequestParam String q,
                                                @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(drugService.suggestNames(q, limit));
    }

    /**
     * Trả toàn bộ danh sách (không phân trang) — chỉ dùng khi thật sự cần (danh sách nhỏ).
     * GET /api/drugs/all
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public ResponseEntity<List<Drug>> getAllDrugs() {
        return ResponseEntity.ok(drugService.getAllDrugs());
    }

    // READ by id
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public ResponseEntity<Drug> getDrug(@PathVariable Long id) {
        return ResponseEntity.ok(drugService.getDrugById(id));
    }

//    // CREATE — chỉ ADMIN
//    @PostMapping
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<Drug> createDrug(@RequestBody Drug drug) {
//        return ResponseEntity.ok(drugService.createDrug(drug));
//    }

    // UPDATE — ADMIN / MODERATOR
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    public ResponseEntity<Drug> updateDrug(
            @PathVariable Long id,
            @RequestPart("drug") String drugJson,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        Drug updatedDrug = mapper.readValue(drugJson, Drug.class);

        return ResponseEntity.ok(
                drugService.updateDrugWithImage(id, updatedDrug, image)
        );
    }

    // UPDATE ACTIVE — ADMIN
    @PutMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Drug> updateDrugActive(
            @PathVariable Long id,
            @RequestBody UpdateDrugActiveRequest request
    ) {
        Drug updatedDrug = drugService.updateDrugActive(id, request.isActive());
        return ResponseEntity.ok(updatedDrug);
    }
    // DELETE — chỉ ADMIN
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDrug(@PathVariable Long id) {
        drugService.deleteDrug(id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Drug> createDrugWithImage(
            @RequestPart("drug") String drugJson,
            @RequestPart("image") MultipartFile image
    ) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        Drug drug = mapper.readValue(drugJson, Drug.class);

        System.out.println(">>> CREATE DRUG HIT <<<");
        return ResponseEntity.ok(drugService.createDrugWithImage(drug, image));
    }


}
